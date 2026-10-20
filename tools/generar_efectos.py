import numpy as np, wave, subprocess, sys, os
OUT=sys.argv[1]; os.makedirs(OUT, exist_ok=True)
SR=44100; rng=np.random.default_rng(7)
def burst(dur, decay, lo, hi):
    n=int(SR*dur); x=rng.standard_normal(n)
    f=np.fft.rfft(x); fr=np.fft.rfftfreq(n,1/SR); f[(fr<lo)|(fr>hi)]=0; x=np.fft.irfft(f,n)
    env=np.exp(-np.arange(n)/(SR*decay)); env[:int(SR*0.002)]*=np.linspace(0,1,int(SR*0.002))
    return x*env
def save(name, x):
    x=x/ (np.abs(x).max()+1e-9)*0.8
    w=os.path.join(OUT, name+'.wav')
    with wave.open(w,'wb') as f:
        f.setnchannels(1); f.setsampwidth(2); f.setframerate(SR); f.writeframes((x*32767).astype('<i2').tobytes())
    subprocess.run(['ffmpeg','-y','-loglevel','error','-i',w,'-c:a','libvorbis','-q:a','5',os.path.join(OUT,name+'.ogg')],check=True); os.remove(w)
# carta sobre el tapete: golpe seco de papel
save('card_play', np.concatenate([burst(0.09,0.018,900,6500)+0.4*burst(0.09,0.03,120,500), np.zeros(800)]))
# mezclar: ráfaga de roces
sh=np.zeros(int(SR*0.9))
for i in range(14):
    s=int(SR*(0.05+i*0.055+rng.uniform(0,0.01))); b=burst(0.05,0.02,1500,8000)*rng.uniform(0.5,1)
    sh[s:s+len(b)]+=b[:len(sh)-s]
save('card_shuffle', sh)
# poroto cayendo en la mesa
t=np.arange(int(SR*0.12))/SR
save('poroto', np.sin(2*np.pi*1900*t)*np.exp(-t/0.012)+0.3*burst(0.12,0.01,2000,9000))
