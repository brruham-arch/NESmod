import os, re

dst = 'app/src/main/cpp/nes/src/APU'
for fn in os.listdir(dst):
    if fn.endswith('.cpp'):
        path = os.path.join(dst, fn)
        txt = open(path).read()
        # Hapus SFML includes
        txt = re.sub(r'#include\s*<SFML/[^>]+>\n?', '', txt)
        open(path, 'w').write(txt)
        print(f'Patched SFML: {fn}')

# Fix APU.cpp FrameCounter initializer list issue
# APU.cpp:288 - FrameCounter({...}, irq) -> FrameCounter(std::vector<...>{...}, irq)
apu = os.path.join(dst, 'APU.cpp')
txt = open(apu).read()
# Wrap initializer list with explicit vector type
txt = txt.replace(
    'return FrameCounter(\n        {',
    'return FrameCounter(\n        std::vector<std::reference_wrapper<FrameClockable>>{'
)
open(apu, 'w').write(txt)
print('Fixed FrameCounter initializer')
