import os, re

dst_src = 'app/src/main/cpp/nes/src/APU'
dst_inc = 'app/src/main/cpp/nes/include/APU'

# 1. Hapus SFML includes
for fn in os.listdir(dst_src):
    if fn.endswith('.cpp'):
        path = os.path.join(dst_src, fn)
        txt = open(path).read()
        txt = re.sub(r'#include\s*<SFML/[^>]+>\n?', '', txt)
        open(path, 'w').write(txt)

# 2. Patch FrameCounter.h — ganti constructor dengan variadic template
fc = os.path.join(dst_inc, 'FrameCounter.h')
txt = open(fc).read()

# Hapus patch sebelumnya jika ada
txt = re.sub(r'#include <initializer_list>\n', '', txt)
txt = re.sub(r'#include <functional>\n', '', txt)

# Ganti constructor initializer_list dan vector dengan variadic template
# Hapus kedua constructor lama
txt = re.sub(
    r'FrameCounter\(std::initializer_list.*?irq\)\s*\{[^}]*\}\s*',
    '',
    txt, flags=re.DOTALL
)

# Tambah template constructor sebelum vector constructor
old = 'FrameCounter(std::vector<std::reference_wrapper<FrameClockable>> slots, IRQHandle& irq)'
new = '''template<typename... Args>
    FrameCounter(IRQHandle& irq, Args&... args)
        : FrameCounter(std::vector<std::reference_wrapper<FrameClockable>>{
            static_cast<FrameClockable&>(args)...}, irq) {}
    FrameCounter(std::vector<std::reference_wrapper<FrameClockable>> slots, IRQHandle& irq)'''

txt = txt.replace(old, new)

# Tambah include vector dan initializer_list
if '#include <vector>' not in txt:
    txt = txt.replace('#pragma once', '#pragma once\n#include <vector>\n#include <functional>')

open(fc, 'w').write(txt)
print('FrameCounter.h patched with variadic template')

# 3. Patch APU.cpp — ganti call site agar pakai versi baru
# Original: return FrameCounter({std::ref(a), std::ref(b), ...}, irq)
# New:      return FrameCounter(irq, a, b, ...)
apu = os.path.join(dst_src, 'APU.cpp')
txt = open(apu).read()

# Cari blok return FrameCounter( ... )
# dan ganti {std::ref(x), ...}, irq dengan irq, x, ...
def fix_frame_counter(m):
    inner = m.group(0)
    # Ekstrak items dalam {}
    brace_match = re.search(r'\{([^}]+)\}', inner, re.DOTALL)
    irq_match = re.search(r'\},\s*(\w+)\s*\)', inner, re.DOTALL)
    if brace_match and irq_match:
        items_raw = brace_match.group(1)
        irq_var = irq_match.group(1)
        # Hapus std::ref() wrapping
        items = re.findall(r'std::ref\(([^)]+)\)', items_raw)
        if items:
            args = ', '.join(items)
            return f'return FrameCounter({irq_var}, {args})'
    return m.group(0)

txt = re.sub(
    r'return FrameCounter\(\s*\{[^}]+\},\s*\w+\s*\)',
    fix_frame_counter,
    txt,
    flags=re.DOTALL
)
open(apu, 'w').write(txt)
print('APU.cpp call site patched')
