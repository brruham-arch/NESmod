import os, re

dst = 'app/src/main/cpp/nes/src/APU'

# 1. Hapus SFML includes dari semua APU files
for fn in os.listdir(dst):
    if fn.endswith('.cpp'):
        path = os.path.join(dst, fn)
        txt = open(path).read()
        txt = re.sub(r'#include\s*<SFML/[^>]+>\n?', '', txt)
        open(path, 'w').write(txt)
        print(f'Patched SFML: {fn}')

# 2. Fix APU.cpp FrameCounter — ganti initializer_list dengan push_back
apu = os.path.join(dst, 'APU.cpp')
txt = open(apu).read()

# Cari dan ganti blok: return FrameCounter(\n    {item1, item2, ...},\n    irq\n);
# Ganti dengan versi yang pakai vector push_back
old = re.search(r'return FrameCounter\(\s*\{([^}]+)\},\s*(\w+)\s*\);', txt, re.DOTALL)
if old:
    items_raw = old.group(1)
    irq_var = old.group(2)
    # Parse items: pisahkan per koma, strip whitespace
    items = [i.strip() for i in items_raw.split(',') if i.strip()]
    pushbacks = '\n    '.join([f'slots.push_back({i});' for i in items])
    replacement = f'''{{
    std::vector<std::reference_wrapper<FrameClockable>> slots;
    {pushbacks}
    return FrameCounter(slots, {irq_var});
}}'''
    # Cari fungsi yang mengandung return FrameCounter
    txt = re.sub(
        r'return FrameCounter\(\s*\{[^}]+\},\s*\w+\s*\);',
        replacement,
        txt,
        flags=re.DOTALL
    )
    print('Fixed FrameCounter with push_back')
else:
    print('WARNING: FrameCounter pattern not found!')

open(apu, 'w').write(txt)
