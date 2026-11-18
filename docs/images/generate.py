#!/usr/bin/env python3
"""Deterministic code-based promotional SVGs/PNGs. No network or device capture.
Requires fontTools plus system librsvg/cairo (PNG export). Run from any directory.
"""
from pathlib import Path
import ctypes as C
import html
import re
import xml.etree.ElementTree as ET
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from fontTools.pens.svgPathPen import SVGPathPen

REPO = Path(__file__).resolve().parents[2]
OUT = Path(__file__).resolve().parent
SRC = REPO / 'app/src/main/java/com/matrix/launcher'
RES = REPO / 'app/src/main/res'
A = '{http://schemas.android.com/apk/res/android}'
# Read the defaults rather than maintaining a separate promotional palette.
tokens = (SRC / 'MatrixTheme.kt').read_text()
def color(name):
    return '#' + re.search(r'DEFAULT_' + name + r' = 0xFF([0-9A-Fa-f]{6})', tokens)[1]
BG, PANEL, SURFACE, TEXT, SECONDARY = [color(n) for n in ['BACKGROUND','PANEL','SURFACE','TEXT','TEXT_SECONDARY']]
RADIUS = float(re.search(r'DEFAULT_RADIUS_DP = ([0-9.]+)f', tokens)[1])
GAP = float(re.search(r'DEFAULT_GAP_DP = ([0-9.]+)f', tokens)[1])
CARD_RADIUS = RADIUS * 14 / 24
fonts = {}
for weight in (300, 400, 500):
    font = TTFont(RES / 'font/outfit.ttf')
    if 'fvar' in font:
        font = instantiateVariableFont(font, {'wght': weight}, inplace=True)
    fonts[weight] = font

def text(x, y, value, size=17, fill=TEXT, weight=400):
    """Outline bundled Outfit for identical rendering without installed fonts."""
    font = fonts[weight]
    cmap, glyphs = font.getBestCmap(), font.getGlyphSet()
    units = font['head'].unitsPerEm
    pen = SVGPathPen(glyphs)
    advance = 0
    from fontTools.pens.transformPen import TransformPen
    for char in value:
        if char == '★':
            # Android supplies a fallback star glyph; Outfit lacks U+2605.
            # Outline the five-point symbol so export never shows a tofu box.
            import math
            points=[(advance+480+440*math.cos(-math.pi/2+i*math.pi/5)*(1 if i%2==0 else .4),440+440*math.sin(-math.pi/2+i*math.pi/5)*(1 if i%2==0 else .4)) for i in range(10)]
            pen.moveTo(points[0])
            for point in points[1:]: pen.lineTo(point)
            pen.closePath(); advance += 980
            continue
        name = cmap.get(ord(char), '.notdef')
        glyphs[name].draw(TransformPen(pen, (1,0,0,1,advance,0)))
        advance += font['hmtx'][name][0]
    return f'<g aria-label="{html.escape(value, quote=True)}"><path fill="{fill}" transform="translate({x} {y}) scale({size/units} {-size/units})" d="{pen.getCommands()}"/></g>'

def rect(x,y,w,h,fill,r=0,stroke=None,sw=1):
    return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="{fill}"' + (f' stroke="{stroke}" stroke-width="{sw}"' if stroke else '') + '/>'

def vector(name,x,y,w,h=None,folder='drawable'):
    tree=ET.parse(RES / folder / (name+'.xml')).getroot()
    vw,vh=float(tree.get(A+'viewportWidth')),float(tree.get(A+'viewportHeight'))
    h=h or w*vh/vw
    parts=[]
    for p in tree.findall('path'):
        fill=p.get(A+'fillColor','#FFFFFF')
        opacity=1
        if len(fill)==9:
            opacity=int(fill[1:3],16)/255;fill='#'+fill[3:]
        parts.append(f'<path fill="{fill}" fill-opacity="{opacity}" d="{p.get(A+"pathData")}"/>')
    return f'<g transform="translate({x} {y}) scale({w/vw} {h/vh})">'+''.join(parts)+'</g>'

def brand(x,y,size):
    return vector('matrix_banner',x,y,size,size*180/320)

def sample_card(x,y,w,h,title,score,portrait=False,progress=None):
    # Plain placeholder tile: no invented movie poster, provider logo or catalog claim.
    s=rect(x,y,w,h,SURFACE,CARD_RADIUS)
    s+=text(x+14,y+h/2,'ARTWORK',12,SECONDARY,500)
    s+=text(x+14,y+h/2+18,'PLACEHOLDER',11,SECONDARY)
    s+=rect(x+8,y+8,43,24,SURFACE,12)+text(x+16,y+24,'FILM',11,TEXT,500)
    badge='★ '+score
    s+=rect(x+w-64,y+8,56,24,'#111524',12,'#F59E0B',.8)
    s+=text(x+w-57,y+24,badge,11,'#FFD166',500)
    if progress is not None:
        s+=rect(x+10,y+h-10,w-20,3,PANEL)+rect(x+10,y+h-10,(w-20)*progress,3,TEXT)
    s+=text(x+8,y+h+24,title,16,TEXT,500)
    s+=text(x+8,y+h+44,'Example metadata',12,SECONDARY)
    return s

def home():
    # An illustrative 1280 x 900 dp viewport, not a claim about TV density.
    s=rect(0,0,1280,900,BG)
    s+=text(48,49,'WELCOME HOME',15,TEXT,500)
    s+=text(48,108,'8:30 PM',56,TEXT,300)
    s+=text(48,134,'TUESDAY 17 NOVEMBER',14,SECONDARY)
    for i,(icon,label) in enumerate([('ic_search','Search'),('ic_input','Inputs'),('ic_wifi','Connected'),('ic_home_refresh','Refresh'),('ic_apps','All apps'),('ic_settings','Settings')]):
        x=842+i*64
        s+=rect(x,34,54,54,'#1F1F1F',27)
        s+=vector(icon,x+15,49,24,24)
        s+=text(x+2,111,label,11,SECONDARY,500)
    s+=text(55,256,'FAVORITES · HOLD OK TO EDIT',12,SECONDARY,500)
    s+=text(55,301,'Stremio',42,TEXT,300)
    # MatrixHomeSurface: outer inset 46, dock inset 12; five fitted slots + 16dp gaps.
    s+=rect(46,327,1188,144,'url(#glass)',28)
    w=(1280-116-4*GAP)/5
    for i,label in enumerate(['Stremio','Nuvio','Example app','Example app','Example app']):
        x=58+i*(w+GAP)
        # Fallback artwork aspect 2.5, see MatrixAppCard.
        s+=rect(x+10,349,w-20,(w-20)/2.5,SURFACE,CARD_RADIUS)
        s+=vector('ic_apps',x+w/2-16,369,32,32)
        if i==0:
            s+=rect(x+9,348,w-18,(w-20)/2.5+2,'none',CARD_RADIUS,TEXT,2)
        s+=text(x+w/2-len(label)*3.3,449,label,14,TEXT)
    s+=text(55,519,'Example app · Continue watching',22,TEXT,500)
    for i,(title,score,progress) in enumerate([('Example film','8.7',.42),('Example series','7.6',.66),('Example film','8.1',.18)]):
        s+=sample_card(58+i*236,539,220,124,title,score,progress=progress)
    s+=text(55,752,'SIMKL · Plan to watch movies',22,TEXT,500)
    for i,score in enumerate(['8.7','7.6','8.1','7.9','8.3','7.8','8.0']):
        s+=sample_card(58+i*166,772,144,216,'Example title',score,True)
    return s

DEFS='<defs><linearGradient id="glass" x1="0" y1="0" x2="0" y2="1"><stop stop-color="white" stop-opacity=".15"/><stop offset=".5" stop-color="white" stop-opacity=".035"/><stop offset="1" stop-color="white" stop-opacity=".08"/></linearGradient></defs>'
def svg(w,h,body,label):
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}" role="img"><title>{html.escape(label)}</title>'+DEFS+body+'</svg>'

hero=rect(0,0,1280,360,BG)+brand(50,56,400)
hero+=text(490,150,'Matrix Launcher',56,TEXT,300)
hero+=text(493,201,'Your apps. Your Home screen.',26,SECONDARY)
hero+=text(493,245,'Google TV · Android TV · Fire TV',18,SECONDARY)
hero+=text(52,331,'Brand composition from the existing Matrix vector art',14,SECONDARY)

# Home image shows the full illustrative content extent. It is intentionally taller
# than a standard TV viewport; the bottom SIMKL row would be reached by scrolling.
home_body=home().replace('height="900"','height="1100"',1)
home_body+=rect(0,1048,1280,52,BG)+text(48,1080,'CODE-BASED MOCKUP · placeholder artwork, titles, scores and progress · not a device screenshot',15,SECONDARY)

# Rasterize the same SVG source with system librsvg/cairo, avoiding browser/font variance.
rsvg=C.CDLL('librsvg-2.so.2'); cairo=C.CDLL('libcairo.so.2'); gobj=C.CDLL('libgobject-2.0.so.0')
rsvg.rsvg_handle_new_from_data.argtypes=[C.c_char_p,C.c_size_t,C.c_void_p];rsvg.rsvg_handle_new_from_data.restype=C.c_void_p
rsvg.rsvg_handle_render_cairo.argtypes=[C.c_void_p,C.c_void_p];rsvg.rsvg_handle_render_cairo.restype=C.c_int
cairo.cairo_image_surface_create.argtypes=[C.c_int,C.c_int,C.c_int];cairo.cairo_image_surface_create.restype=C.c_void_p
cairo.cairo_create.argtypes=[C.c_void_p];cairo.cairo_create.restype=C.c_void_p
cairo.cairo_surface_write_to_png.argtypes=[C.c_void_p,C.c_char_p];cairo.cairo_surface_write_to_png.restype=C.c_int
cairo.cairo_destroy.argtypes=[C.c_void_p];cairo.cairo_surface_destroy.argtypes=[C.c_void_p];gobj.g_object_unref.argtypes=[C.c_void_p]
for name,w,h,body,label in [('matrix-hero',1280,360,hero,'Matrix Launcher brand'),('matrix-home-mockup',1280,1100,home_body,'Code-based Matrix Home mockup, not a screenshot')]:
    source=svg(w,h,body,label).encode()
    (OUT/(name+'.svg')).write_bytes(source)
    handle=rsvg.rsvg_handle_new_from_data(source,len(source),None)
    assert handle,'Invalid generated SVG'
    surface=cairo.cairo_image_surface_create(0,w,h); ctx=cairo.cairo_create(surface)
    assert rsvg.rsvg_handle_render_cairo(handle,ctx),'SVG rendering failed'
    assert cairo.cairo_surface_write_to_png(surface,str(OUT/(name+'.png')).encode())==0
    cairo.cairo_destroy(ctx);cairo.cairo_surface_destroy(surface);gobj.g_object_unref(handle)
    print(name,w,h)
