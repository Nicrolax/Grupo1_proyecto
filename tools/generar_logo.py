"""
Logo del repositorio (docs/logo.png) e ícono del mod (src/main/resources/trucoya_logo.png): el ancho de espada,
el ancho de basto y el siete de oro de la baraja criolla del mod, en abanico sobre un paño verde con aro dorado.
Uso: python3 tools/generar_logo.py   (desde la raíz del proyecto)
"""
from PIL import Image, ImageDraw, ImageFilter, ImageFont
import math, random
A=Image.open('src/main/resources/assets/trucoya/textures/gui/cartas_criolla.png').convert('RGBA')
def carta(fila,col): return A.crop((col*128,fila*192,col*128+128,fila*192+192))
S=1024; c=S//2; R=480
VERDE=(22,88,56,255); OSCURO=(110,74,24,255); ORO=(232,184,42,255)
def base(texto):
    im=Image.new('RGBA',(S,S),(0,0,0,0))
    sh=Image.new('RGBA',(S,S),(0,0,0,0)); ImageDraw.Draw(sh).ellipse((c-R+8,c-R+20,c+R+8,c+R+20),fill=(0,0,0,110))
    im=Image.alpha_composite(im,sh.filter(ImageFilter.GaussianBlur(12)))
    d=ImageDraw.Draw(im)
    d.ellipse((c-R,c-R,c+R,c+R),fill=OSCURO)
    d.ellipse((c-R+12,c-R+12,c+R-12,c+R-12),fill=ORO)
    d.ellipse((c-R+28,c-R+28,c+R-28,c+R-28),fill=OSCURO)
    d.ellipse((c-R+36,c-R+36,c+R-36,c+R-36),fill=VERDE)
    # paño: puntos sutiles + viñeta
    random.seed(3); p=Image.new('RGBA',(S,S),(0,0,0,0)); pd=ImageDraw.Draw(p)
    for _ in range(12000):
        x=random.randint(0,S-1); y=random.randint(0,S-1)
        if (x-c)**2+(y-c)**2<(R-40)**2: pd.point((x,y),fill=random.choice([(14,70,44,90),(36,112,72,70)]))
    im=Image.alpha_composite(im,p)
    g=Image.new('RGBA',(S,S),(0,0,0,0)); gd=ImageDraw.Draw(g)
    for i in range(80):
        rr=R-36-i; gd.ellipse((c-rr,c-rr,c+rr,c+rr),outline=(0,0,0,int(120*(1-i/80)**2)),width=2)
    im=Image.alpha_composite(im,g)
    return im
def abanico(im, cy, alto):
    W=int(alto*2/3); H=alto
    pivote=(c, cy+int(H*0.70))
    orden=[(carta(2,6),-30),(carta(1,0),30),(carta(0,0),0)]   # 7 de oro, ancho de basto, ancho de espada arriba
    for cd,ang in orden:
        cc=cd.resize((W,H),Image.LANCZOS)
        L=3*H; lienzo=Image.new('RGBA',(L,L),(0,0,0,0))
        # la carta con su base cerca del centro del lienzo (el pivote)
        bx=L//2-W//2; by=L//2-int(H*1.12)
        som=Image.new('RGBA',(L,L),(0,0,0,0)); ImageDraw.Draw(som).rounded_rectangle((bx+6,by+12,bx+W+6,by+H+12),int(W*0.08),fill=(0,0,0,150))
        som=som.filter(ImageFilter.GaussianBlur(9))
        lienzo=Image.alpha_composite(som,lienzo); lienzo.alpha_composite(cc,(bx,by))
        lienzo=lienzo.rotate(ang,resample=Image.BICUBIC,center=(L//2,L//2))
        im.alpha_composite(lienzo,(pivote[0]-L//2,pivote[1]-L//2))
    return im
def cinta(im, texto, y):
    d=ImageDraw.Draw(im)
    f=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf',92)
    tw=d.textlength(texto,font=f); w=int(tw+110); h=128
    x0=c-w//2
    # cinta con colas
    d.polygon([(x0-50,y+22),(x0+10,y+22),(x0+10,y+h+22),(x0-50,y+h+22),(x0-20,y+h//2+22)],fill=(150,30,36,255))
    d.polygon([(x0+w+50,y+22),(x0+w-10,y+22),(x0+w-10,y+h+22),(x0+w+50,y+h+22),(x0+w+20,y+h//2+22)],fill=(150,30,36,255))
    d.rounded_rectangle((x0,y,x0+w,y+h),14,fill=(196,44,48,255),outline=ORO,width=6)
    d.text((c+3,y+h//2+4),texto,font=f,fill=(70,10,14,255),anchor='mm')
    d.text((c,y+h//2),texto,font=f,fill=(255,244,214,255),anchor='mm')
    return im
# ícono (sin texto) y logo con la cinta
ico = abanico(base(None), c - 10, 420)
log = cinta(abanico(base(None), c - 60, 380), 'TRUCO YA', c + 190)
log.resize((512, 512), Image.LANCZOS).save('docs/logo.png', optimize=True)
ico.resize((256, 256), Image.LANCZOS).save('src/main/resources/trucoya_logo.png', optimize=True)
