import os, re

dst = 'app/src/main/cpp/nes/src/APU'
for fn in os.listdir(dst):
    if fn.endswith('.cpp'):
        path = os.path.join(dst, fn)
        txt = open(path).read()
        txt = re.sub(r'#include\s*<SFML/[^>]+>', '', txt)
        open(path, 'w').write(txt)
        print(f'Patched: {fn}')
