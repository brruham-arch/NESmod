#!/usr/bin/env python3
"""
NESMod Termux Client — kontrol NES dari Termux via TCP socket.
Usage: python3 termux_client.py

Commands:
  status              - lihat lives, power, world, coins
  read <hex_addr>     - baca RAM
  write <hex> <hex>   - tulis RAM
  lives <n>           - set lives
  fire / big / small  - ubah power Mario
  warp <world> <lvl>  - warp (1-based)
  coins <n>           - set coins
  reset               - reset game
  quit                - keluar
"""
import socket, sys

HOST, PORT = '127.0.0.1', 7890

ADDR = {
    'lives': 0x075A, 'power': 0x0756,
    'world': 0x075F, 'level': 0x075C,
    'coins': 0x07ED, 'mario_x': 0x0086,
    'mario_y': 0x00CE,
    'enemy0': 0x0016, 'enemy1': 0x0017, 'enemy2': 0x0018,
}

def cmd(s, c):
    s.sendall((c+'\n').encode())
    return s.recv(256).decode().strip()

def rram(s, a):
    r = cmd(s, f'READ {a:04X}')
    return int(r.split()[1], 16) if r.startswith('VALUE') else None

def wram(s, a, v):
    return cmd(s, f'WRITE {a:04X} {v:02X}') == 'OK'

def status(s):
    lives = rram(s, ADDR['lives'])
    power = {0:'Small',1:'Big',2:'Fire'}.get(rram(s, ADDR['power']), '?')
    world = rram(s, ADDR['world'])
    level = rram(s, ADDR['level'])
    coins = rram(s, ADDR['coins'])
    print(f"  Lives:{lives}  Power:{power}  World:{world+1}-{level+1}  Coins:{coins}")

def main():
    try:
        s = socket.create_connection((HOST, PORT), timeout=3)
        print(f"Connected to NESMod :{PORT}\n")
    except Exception as e:
        print(f"Gagal connect: {e}\nPastikan app NESMod sudah jalan."); sys.exit(1)

    while True:
        try:
            line = input('nes> ').strip()
        except (EOFError, KeyboardInterrupt):
            cmd(s, 'QUIT'); break
        if not line: continue
        p = line.split(); c = p[0].lower()

        if   c == 'status':              status(s)
        elif c == 'read'   and len(p)>1: v=rram(s,int(p[1],16)); print(f"  {int(p[1],16):04X} = {v:02X} ({v})")
        elif c == 'write'  and len(p)>2: wram(s,int(p[1],16),int(p[2],16)); print("  OK")
        elif c == 'lives'  and len(p)>1: wram(s,ADDR['lives'],int(p[1])); print(f"  Lives={p[1]}")
        elif c == 'fire':                wram(s,ADDR['power'],2); print("  Fire Mario!")
        elif c == 'big':                 wram(s,ADDR['power'],1); print("  Big Mario!")
        elif c == 'small':               wram(s,ADDR['power'],0); print("  Small Mario!")
        elif c == 'warp'   and len(p)>2:
            wram(s,ADDR['world'],int(p[1])-1)
            wram(s,ADDR['level'],int(p[2])-1)
            print(f"  Warped to {p[1]}-{p[2]}")
        elif c == 'coins'  and len(p)>1: wram(s,ADDR['coins'],int(p[1])); print(f"  Coins={p[1]}")
        elif c == 'reset':               cmd(s,'RESET'); print("  Reset.")
        elif c in ('quit','exit'):       cmd(s,'QUIT'); break
        elif c == 'help':                print(__doc__)
        else:                            print(f"  Unknown: {c}")
    s.close()

if __name__ == '__main__': main()
