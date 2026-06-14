import os, re

dst = 'app/src/main/cpp/nes/src/APU'
for fn in os.listdir(dst):
    if fn.endswith('.cpp'):
        path = os.path.join(dst, fn)
        txt = open(path).read()
        txt = re.sub(r'#include\s*<SFML/[^>]+>\n?', '', txt)
        open(path, 'w').write(txt)
        print(f'Patched SFML: {fn}')

# Fix APU.cpp FrameCounter — brace initializer list tidak bisa implisit ke vector
apu = os.path.join(dst, 'APU.cpp')
txt = open(apu).read()

# Ganti: return FrameCounter(\n        {
# Dengan: return FrameCounter(\n        std::vector<std::reference_wrapper<sn::FrameClockable>>{
txt = re.sub(
    r'return FrameCounter\(\s*\{',
    'return FrameCounter(\n        std::vector<std::reference_wrapper<FrameClockable>>{',
    txt
)
open(apu, 'w').write(txt)
print('Fixed FrameCounter initializer list')
