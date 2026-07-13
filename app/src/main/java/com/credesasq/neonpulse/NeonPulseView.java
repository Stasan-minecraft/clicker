package com.credesasq.neonpulse;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Random;

public class NeonPulseView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random rng = new Random();
    private final ArrayList<Orb> orbs = new ArrayList<>();
    private final ArrayList<Particle> particles = new ArrayList<>();
    private final SharedPreferences prefs;
    private float w,h,cx,cy,px,py,tx,ty,pulse;
    private long last,start,lastSpawn,shieldUntil;
    private int score,best,combo,lives=3;
    private boolean started,over,drag;

    static class Orb { float x,y,r,vx,vy; int t; Orb(float x,float y,float r,float vx,float vy,int t){this.x=x;this.y=y;this.r=r;this.vx=vx;this.vy=vy;this.t=t;} }
    static class Particle { float x,y,vx,vy,life,size; Particle(float x,float y,float vx,float vy,float life,float size){this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.life=life;this.size=size;} }

    public NeonPulseView(Context c){ super(c); prefs=c.getSharedPreferences("save",0); best=prefs.getInt("best",0); setLayerType(View.LAYER_TYPE_SOFTWARE,null); }
    protected void onSizeChanged(int W,int H,int ow,int oh){w=W;h=H;cx=w/2f;cy=h*.62f;px=tx=cx;py=ty=cy;}
    private void reset(){orbs.clear();particles.clear();score=combo=0;lives=3;started=true;over=false;px=tx=cx;py=ty=cy;start=System.currentTimeMillis();lastSpawn=0;shieldUntil=0;}

    protected void onDraw(Canvas c){
        long now=System.currentTimeMillis(); float dt=last==0?.016f:Math.min(.034f,(now-last)/1000f); last=now;
        update(dt,now); drawBackground(c,now); drawObjects(c,now); drawUi(c); invalidate();
    }

    private void update(float dt,long now){
        pulse+=dt; if(!started||over)return;
        px+=(tx-px)*Math.min(1,dt*12); py+=(ty-py)*Math.min(1,dt*12);
        float difficulty=1f+(now-start)/22000f;
        if(now-lastSpawn>Math.max(260,760/difficulty)){spawn(difficulty);lastSpawn=now;}
        for(int i=orbs.size()-1;i>=0;i--){
            Orb o=orbs.get(i); o.x+=o.vx*dt;o.y+=o.vy*dt;
            float dx=o.x-px,dy=o.y-py;
            if(dx*dx+dy*dy<(o.r+34)*(o.r+34)){
                if(o.t==1){score+=15+combo*2;combo++;burst(o.x,o.y,18);orbs.remove(i);continue;}
                if(o.t==2){shieldUntil=now+4500;score+=25;burst(o.x,o.y,24);orbs.remove(i);continue;}
                if(now<shieldUntil){score+=5;burst(o.x,o.y,12);orbs.remove(i);continue;}
                lives--;combo=0;burst(px,py,30);vibrate();orbs.remove(i);
                if(lives<=0){over=true;if(score>best){best=score;prefs.edit().putInt("best",best).apply();}} continue;
            }
            if(o.x<-130||o.x>w+130||o.y<-130||o.y>h+130){if(o.t==1)combo=0;orbs.remove(i);}
        }
        for(int i=particles.size()-1;i>=0;i--){Particle q=particles.get(i);q.x+=q.vx*dt;q.y+=q.vy*dt;q.vx*=.985f;q.vy*=.985f;q.life-=dt;if(q.life<=0)particles.remove(i);}
        score+=(int)(dt*8*difficulty);
    }

    private void spawn(float d){
        int side=rng.nextInt(4);float x,y;
        if(side==0){x=rng.nextFloat()*w;y=-50;}else if(side==1){x=w+50;y=rng.nextFloat()*h;}else if(side==2){x=rng.nextFloat()*w;y=h+50;}else{x=-50;y=rng.nextFloat()*h;}
        float dx=cx-x+(rng.nextFloat()-.5f)*w*.35f,dy=cy-y+(rng.nextFloat()-.5f)*h*.35f,len=(float)Math.sqrt(dx*dx+dy*dy);
        float speed=(170+rng.nextFloat()*100)*d;int roll=rng.nextInt(100),type=roll<18?1:(roll<22?2:0);float r=type==0?18+rng.nextFloat()*16:16+rng.nextFloat()*10;
        orbs.add(new Orb(x,y,r,dx/len*speed,dy/len*speed,type));
    }
    private void burst(float x,float y,int n){for(int i=0;i<n;i++){float a=rng.nextFloat()*6.283f,s=70+rng.nextFloat()*260;particles.add(new Particle(x,y,(float)Math.cos(a)*s,(float)Math.sin(a)*s,.35f+rng.nextFloat()*.55f,3+rng.nextFloat()*7));}}
    private void vibrate(){try{Vibrator v=(Vibrator)getContext().getSystemService(Context.VIBRATOR_SERVICE);if(android.os.Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createOneShot(70,150));else v.vibrate(70);}catch(Exception ignored){}}

    private void drawBackground(Canvas c,long now){
        p.setShader(new LinearGradient(0,0,0,h,Color.rgb(4,8,24),Color.rgb(13,4,35),Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(Color.argb(22,99,243,255));float off=(now%2500)/2500f*70;
        for(float x=-h;x<w+h;x+=70)c.drawLine(x+off,0,x-h+off,h,p);p.setStyle(Paint.Style.FILL);
    }
    private void drawObjects(Canvas c,long now){
        for(Particle q:particles){p.setColor(Color.argb((int)(255*Math.max(0,q.life)),99,243,255));c.drawCircle(q.x,q.y,q.size,p);}
        for(Orb o:orbs){int col=o.t==0?Color.rgb(255,72,124):(o.t==1?Color.rgb(255,221,75):Color.rgb(95,255,190));p.setShadowLayer(24,0,0,col);p.setColor(col);c.drawCircle(o.x,o.y,o.r,p);p.clearShadowLayer();}
        int col=now<shieldUntil?Color.rgb(95,255,190):Color.rgb(99,243,255);float r=34+(float)Math.sin(pulse*6)*3;p.setShadowLayer(38,0,0,col);p.setColor(col);c.drawCircle(px,py,r,p);p.clearShadowLayer();p.setColor(Color.WHITE);c.drawCircle(px-10,py-10,8,p);
        if(now<shieldUntil){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);p.setColor(Color.argb(180,95,255,190));c.drawCircle(px,py,55+(float)Math.sin(pulse*8)*4,p);p.setStyle(Paint.Style.FILL);}
    }
    private void text(Canvas c,String s,float x,float y,float size,int color,Paint.Align align){p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextSize(size);p.setTextAlign(align);p.setColor(color);p.clearShadowLayer();c.drawText(s,x,y,p);}
    private void button(Canvas c,String s,float y){p.setColor(Color.rgb(99,243,255));p.setShadowLayer(28,0,0,Color.rgb(99,243,255));c.drawRoundRect(w*.2f,y-58,w*.8f,y+22,40,40,p);p.clearShadowLayer();text(c,s,cx,y-5,26,Color.rgb(4,8,24),Paint.Align.CENTER);}
    private void drawUi(Canvas c){
        if(!started){text(c,"NEON",cx,h*.22f,72,Color.WHITE,Paint.Align.CENTER);text(c,"PULSE",cx,h*.30f,72,Color.rgb(99,243,255),Paint.Align.CENTER);text(c,"DRAG • DODGE • COLLECT",cx,h*.37f,18,Color.LTGRAY,Paint.Align.CENTER);button(c,"START",h*.68f);text(c,"BEST  "+best,cx,h*.82f,24,Color.rgb(255,221,75),Paint.Align.CENTER);return;}
        text(c,"SCORE  "+score,32,72,28,Color.WHITE,Paint.Align.LEFT);text(c,"BEST  "+best,w-32,72,22,Color.LTGRAY,Paint.Align.RIGHT);text(c,"COMBO x"+Math.max(1,combo),32,108,18,Color.rgb(255,221,75),Paint.Align.LEFT);
        for(int i=0;i<3;i++){p.setColor(i<lives?Color.rgb(255,72,124):Color.argb(55,255,255,255));c.drawCircle(w-38-i*36,108,10,p);}
        if(over){p.setColor(Color.argb(215,3,5,18));c.drawRoundRect(w*.08f,h*.22f,w*.92f,h*.78f,38,38,p);text(c,"GAME OVER",cx,h*.34f,42,Color.WHITE,Paint.Align.CENTER);text(c,"SCORE",cx,h*.43f,18,Color.LTGRAY,Paint.Align.CENTER);text(c,String.valueOf(score),cx,h*.53f,74,Color.rgb(99,243,255),Paint.Align.CENTER);button(c,"PLAY AGAIN",h*.66f);}
    }
    public boolean onTouchEvent(MotionEvent e){float x=e.getX(),y=e.getY();if(e.getAction()==MotionEvent.ACTION_DOWN){if(!started||over){reset();return true;}drag=true;tx=x;ty=y;return true;}if(e.getAction()==MotionEvent.ACTION_MOVE&&drag){tx=Math.max(42,Math.min(w-42,x));ty=Math.max(150,Math.min(h-60,y));return true;}if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL){drag=false;return true;}return true;}
}
