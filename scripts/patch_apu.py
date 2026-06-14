import os, re

dst = 'app/src/main/cpp/nes/src/APU'

# 1. Hapus SFML includes
for fn in os.listdir(dst):
    if fn.endswith('.cpp'):
        path = os.path.join(dst, fn)
        txt = open(path).read()
        txt = re.sub(r'#include\s*<SFML/[^>]+>\n?', '', txt)
        open(path, 'w').write(txt)

# 2. Fix APU.cpp — ganti push_back items dengan explicit cast ke FrameClockable&
apu = os.path.join(dst, 'APU.cpp')
txt = open(apu).read()

# Cari blok { slots; push_back; return FrameCounter }
# Ganti semua push_back(std::ref(x)) dengan push_back(static_cast<FrameClockable&>(x))
txt = re.sub(
    r'slots\.push_back\((\w[\w.]+)\);',
    lambda m: f'slots.push_back(static_cast<FrameClockable&>({m.group(1)}));',
    txt
)
print('Fixed push_back with static_cast')
open(apu, 'w').write(txt)
