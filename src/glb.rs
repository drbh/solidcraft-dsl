//! Geometry-only GLB 2.0, from the engine's binary STL tessellation. glTF is Y-up,
//! metres: transform CAD (x,y,z) mm to (x,z,-y)/1000, preserving handedness.
use serde_json::{Value, json};
use std::path::Path;

pub fn convert(stl: &Path, output: &Path) -> Result<Value, String> {
    let data = std::fs::read(stl).map_err(|e| e.to_string())?;
    let bytes = encode(&data)?;
    std::fs::write(output, bytes).map_err(|e| e.to_string())?;
    Ok(
        json!({"source":"binary STL", "units":"metres", "up":"Y", "coordinate_mapping":"[x,z,-y] / 1000", "geometry_only":true, "omits":["materials","CAD features","assembly hierarchy"]}),
    )
}
fn word(data: &[u8], i: usize) -> Result<[u8; 4], String> {
    data.get(i..i + 4).ok_or("truncated STL")?.try_into().map_err(|_| "truncated STL".into())
}
fn encode(data: &[u8]) -> Result<Vec<u8>, String> {
    let n = u32::from_le_bytes(word(data, 80)?) as usize;
    if n == 0 || n.checked_mul(50).and_then(|v| v.checked_add(84)) != Some(data.len()) {
        return Err("GLB conversion requires a nonempty binary STL with a valid triangle count".into());
    }
    let mut positions = Vec::new();
    let mut normals = Vec::new();
    let mut min = [f32::INFINITY; 3];
    let mut max = [f32::NEG_INFINITY; 3];
    for triangle in 0..n {
        let mut p = [[0f32; 3]; 3];
        for (vertex, p) in p.iter_mut().enumerate() {
            let offset = 84 + triangle * 50 + 12 + vertex * 12;
            let x = f32::from_le_bytes(word(data, offset)?) / 1000.;
            let y = f32::from_le_bytes(word(data, offset + 4)?) / 1000.;
            let z = f32::from_le_bytes(word(data, offset + 8)?) / 1000.;
            *p = [x, z, -y];
            for j in 0..3 {
                if !p[j].is_finite() {
                    return Err("non-finite STL coordinates".into());
                }
                min[j] = min[j].min(p[j]);
                max[j] = max[j].max(p[j]);
                positions.extend(p[j].to_le_bytes());
            }
        }
        // Compute unit normals from transformed vertices rather than trusting STL normals.
        let a: [f64; 3] = std::array::from_fn(|j| f64::from(p[1][j]) - f64::from(p[0][j]));
        let b: [f64; 3] = std::array::from_fn(|j| f64::from(p[2][j]) - f64::from(p[0][j]));
        let normal = [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]];
        let length = normal.iter().map(|v| v * v).sum::<f64>().sqrt();
        let normal = if length > 0. { normal.map(|v| (v / length) as f32) } else { [0., 1., 0.] };
        for _ in 0..3 {
            for v in normal {
                normals.extend(v.to_le_bytes());
            }
        }
    }
    let len = positions.len();
    positions.extend(normals);
    let mut doc = serde_json::to_vec(&json!({
        "asset":{"version":"2.0","generator":"solidcraft-dsl"},
        "scene":0,"scenes":[{"nodes":[0]}],"nodes":[{"mesh":0}],
        "meshes":[{"primitives":[{"attributes":{"POSITION":0,"NORMAL":1},"mode":4}]}],
        "buffers":[{"byteLength":positions.len()}],
        "bufferViews":[{"buffer":0,"byteOffset":0,"byteLength":len,"target":34962},{"buffer":0,"byteOffset":len,"byteLength":len,"target":34962}],
        "accessors":[{"bufferView":0,"componentType":5126,"count":n*3,"type":"VEC3","min":min,"max":max}, {"bufferView":1,"componentType":5126,"count":n*3,"type":"VEC3"}]
    })).map_err(|e| e.to_string())?;
    while doc.len() % 4 != 0 {
        doc.push(b' ');
    }
    let total = u32::try_from(12 + 8 + doc.len() + 8 + positions.len()).map_err(|_| "GLB exceeds 4 GiB")?;
    let mut out = Vec::new();
    out.extend(b"glTF");
    out.extend(2u32.to_le_bytes());
    out.extend(total.to_le_bytes());
    out.extend((doc.len() as u32).to_le_bytes());
    out.extend(b"JSON");
    out.extend(doc);
    out.extend((positions.len() as u32).to_le_bytes());
    out.extend(b"BIN\0");
    out.extend(positions);
    Ok(out)
}

#[cfg(test)]
#[allow(clippy::unwrap_used)]
mod tests {
    use super::*;
    #[test]
    fn glb_has_correct_units_axes_bounds_and_aligned_chunks() {
        let mut stl = vec![0; 80];
        stl.extend(1u32.to_le_bytes());
        stl.extend([0; 12]);
        for p in [[0f32, 0., 0.], [1000., 0., 0.], [0., 1000., 2000.]] {
            for x in p {
                stl.extend(x.to_le_bytes());
            }
        }
        stl.extend([0; 2]);
        let glb = encode(&stl).unwrap();
        assert_eq!(&glb[..4], b"glTF");
        assert_eq!(u32::from_le_bytes(word(&glb, 8).unwrap()) as usize, glb.len());
        let n = u32::from_le_bytes(word(&glb, 12).unwrap()) as usize;
        assert_eq!(n % 4, 0);
        let doc: Value = serde_json::from_slice(&glb[20..20 + n]).unwrap();
        assert_eq!(doc["accessors"][0]["min"], json!([0., 0., -1.]));
        let max: Vec<f64> = serde_json::from_value(doc["accessors"][0]["max"].clone()).unwrap();
        assert_eq!(max, [1., 2., 0.]);
        assert_eq!(doc["accessors"][0]["count"], 3);
        assert_eq!(&glb[24 + n..28 + n], b"BIN\0");
        let start = 28 + n;
        assert_eq!(f32::from_le_bytes(word(&glb, start + 12).unwrap()), 1.);
        assert!(encode(&stl[..stl.len() - 1]).is_err());
        assert!(encode(&[]).is_err());
    }
}
