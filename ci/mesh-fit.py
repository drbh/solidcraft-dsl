#!/usr/bin/env python3
"""Fit of two parts measured on their meshes, for when the engine's own clearance query doesn't
finish (docs/engine-performance.md).

    ci/mesh-fit.py A.stl B.stl

Export both bodies with `file.export {"bodies": [...], "quality": "finest"}`. Prints how many
points of each mesh lie inside the other and how deep, and the smallest gap between them. Points
near the other mesh get a signed distance from the closest triangle; the ones that come out
inside are confirmed by ray parity, because the closest triangle's normal is unreliable at sharp
crests. Needs numpy and scipy. Calibrated on example 49: at 180 deg it finds no overlap and a
0.0883 mm gap (the engine measures 0.088 mm exactly); at 330 deg it finds overlap.
"""
import sys, struct, numpy as np
from scipy.spatial import cKDTree
def stl(path):
    b=open(path,'rb').read(); n=struct.unpack('<I',b[80:84])[0]
    a=np.frombuffer(b[84:84+n*50],dtype=np.dtype([('n','<3f4'),('v','<9f4'),('a','<u2')]))
    return a['v'].reshape(-1,3,3).astype(np.float64)
def inside_exact(tris, pts):
    """Ray parity along a skewed +X ray, against every triangle (vectorised per point)."""
    d=np.array([1.0,1.3e-3,2.9e-3]); d/=np.linalg.norm(d)
    v0=tris[:,0]; e1=tris[:,1]-v0; e2=tris[:,2]-v0
    h=np.cross(d,e2); a=(e1*h).sum(1); ok=np.abs(a)>1e-15; f=np.where(ok,1/np.where(ok,a,1),0)
    out=np.zeros(len(pts),bool)
    for i,p in enumerate(pts):
        s=p-v0; u=f*(s*h).sum(1); q=np.cross(s,e1); v=f*(q@d); t=f*(e2*q).sum(1)
        out[i]=(ok&(u>=0)&(v>=0)&(u+v<=1)&(t>1e-9)).sum()%2==1
    return out
def pt_tri(p, t):
    # Distance from points p (k,3) to one triangle t (3,3), Ericson's closest point.
    a,b,c=t; ab=b-a; ac=c-a; ap=p-a
    d1=ap@ab; d2=ap@ac
    bp=p-b; d3=bp@ab; d4=bp@ac; cp=p-c; d5=cp@ab; d6=cp@ac
    va=d3*d6-d5*d4; vb=d5*d2-d1*d6; vc=d1*d4-d3*d2
    denom=1/np.where(va+vb+vc==0,1,va+vb+vc); v=vb*denom; w=vc*denom
    cl=a+np.outer(v,ab)+np.outer(w,ac)
    # edge/vertex regions
    m=(d1<=0)&(d2<=0); cl[m]=a
    m=(d3>=0)&(d4<=d3); cl[m]=b
    m=(d6>=0)&(d5<=d6); cl[m]=c
    vc_m=(vc<=0)&(d1>=0)&(d3<=0); tt=d1/np.where(d1-d3==0,1,d1-d3); cl[vc_m]=a+np.outer(tt,ab)[vc_m]
    vb_m=(vb<=0)&(d2>=0)&(d6<=0); tt=d2/np.where(d2-d6==0,1,d2-d6); cl[vb_m]=a+np.outer(tt,ac)[vb_m]
    va_m=(va<=0)&((d4-d3)>=0)&((d5-d6)>=0); tt=(d4-d3)/np.where((d4-d3)+(d5-d6)==0,1,(d4-d3)+(d5-d6)); cl[va_m]=b+np.outer(tt,c-b)[va_m]
    return np.linalg.norm(p-cl,axis=1)
A=stl(sys.argv[1]); B=stl(sys.argv[2])  # A: e.g. the bottle; B: the cap
def signed(P, T):
    """Signed distance of each point to the closed mesh T: negative inside (by the closest
    triangle's outward normal)."""
    cent=T.mean(1); n=np.cross(T[:,1]-T[:,0],T[:,2]-T[:,0]); n/=np.linalg.norm(n,axis=1)[:,None]+1e-30
    keep=cKDTree(P).query(cent,k=1,distance_upper_bound=2.0)[0]<np.inf
    T,n,cent=T[keep],n[keep],cent[keep]
    tr=cKDTree(cent); out=np.empty(len(P))
    for i,p in enumerate(P):
        _,idx=tr.query(p,k=min(32,len(T)))
        idx=np.atleast_1d(idx); best=np.inf; bj=idx[0]
        for j in idx:
            d=pt_tri(p[None,:],T[j])[0]
            if d<best: best,bj=d,j
        out[i]=best if (p-T[bj][0])@n[bj]>=0 else -best
    return out
pa=np.unique(A.reshape(-1,3),axis=0); pb=np.unique(B.reshape(-1,3),axis=0)
near_b=pb[cKDTree(pa).query(pb,k=1)[0]<1.5]
near_a=pa[cKDTree(pb).query(pa,k=1)[0]<1.5]
sb=signed(near_b,A) if len(near_b) else np.array([np.inf])
sa=signed(near_a,B) if len(near_a) else np.array([np.inf])
# Confirm points the normal test calls inside with exact ray parity (the normal test is wrong
# near sharp crests).
fb=near_b[sb<-1e-3]; fa=near_a[sa<-1e-3]
cb=inside_exact(A,fb) if len(fb) else np.zeros(0,bool)
ca=inside_exact(B,fa) if len(fa) else np.zeros(0,bool)
deep_b=(-sb[sb<-1e-3][cb]).max() if cb.any() else 0.0
deep_a=(-sa[sa<-1e-3][ca]).max() if ca.any() else 0.0
print(f"inside the other: {int(cb.sum())} points of B (deepest {deep_b:.4f} mm), {int(ca.sum())} of A (deepest {deep_a:.4f} mm)")
print(f"points near the other mesh: {len(near_b)} of B, {len(near_a)} of A; "
      f"smallest gap {min(sb[sb>0].min() if (sb>0).any() else np.inf, sa[sa>0].min() if (sa>0).any() else np.inf):.4f} mm")
