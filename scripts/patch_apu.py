import os, re

dst_src = 'app/src/main/cpp/nes/src/APU'
dst_inc = 'app/src/main/cpp/nes/include/APU'

# 1. Hapus SFML includes dari semua APU src
for fn in os.listdir(dst_src):
    if fn.endswith('.cpp'):
        path = os.path.join(dst_src, fn)
        txt = open(path).read()
        txt = re.sub(r'#include\s*<SFML/[^>]+>\n?', '', txt)
        open(path, 'w').write(txt)
        print(f'SFML patched: {fn}')

# 2. Patch FrameCounter.h — tambah constructor yang accept initializer_list
fc = os.path.join(dst_inc, 'FrameCounter.h')
txt = open(fc).read()

# Tambah #include <initializer_list> dan constructor overload
if 'initializer_list' not in txt:
    txt = txt.replace(
        '#pragma once',
        '#pragma once\n#include <initializer_list>\n#include <functional>'
    )
    # Tambah constructor overload sebelum constructor yang ada
    old_ctor = 'FrameCounter(std::vector<std::reference_wrapper<FrameClockable>> slots, IRQHandle& irq)'
    new_ctor = '''FrameCounter(std::initializer_list<std::reference_wrapper<FrameClockable>> il, IRQHandle& irq)
        : FrameCounter(std::vector<std::reference_wrapper<FrameClockable>>(il.begin(), il.end()), irq) {}
    ''' + old_ctor
    txt = txt.replace(old_ctor, new_ctor)
    open(fc, 'w').write(txt)
    print('FrameCounter.h patched with initializer_list constructor')
else:
    print('FrameCounter.h already patched')
