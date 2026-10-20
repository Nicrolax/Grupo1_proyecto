"""
Jueces automáticos de calidad de voz (para elegir la mejor toma de cada canto sin oírla):
  DNSMOS (Microsoft, MIT): predice la nota que le pondrían oyentes humanos (escala P.808, 1 a 5).
Los modelos se bajan solos desde GitHub (microsoft/DNS-Challenge) a tools/.modelos/dnsmos.
"""
import os, urllib.request
import numpy as np

_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".modelos", "dnsmos")
_URL = "https://raw.githubusercontent.com/microsoft/DNS-Challenge/master/DNSMOS/DNSMOS/{}"
_sesiones = None


def _cargar():
    global _sesiones
    if _sesiones is None:
        import onnxruntime as ort
        os.makedirs(_DIR, exist_ok=True)
        for f in ("sig_bak_ovr.onnx", "model_v8.onnx"):
            p = os.path.join(_DIR, f)
            if not os.path.exists(p):
                urllib.request.urlretrieve(_URL.format(f), p)
        _sesiones = (ort.InferenceSession(os.path.join(_DIR, "sig_bak_ovr.onnx")),
                     ort.InferenceSession(os.path.join(_DIR, "model_v8.onnx")))
    return _sesiones


def nota(x, sr):
    """Nota de 'oyente' (P.808) de un audio mono."""
    import librosa
    sig, p808 = _cargar()
    a = librosa.resample(np.asarray(x, dtype=np.float32), orig_sr=sr, target_sr=16000)
    largo = int(9.01 * 16000)
    while len(a) < largo:
        a = np.concatenate([a, np.zeros(1600, dtype=np.float32), a])
    a = a[:largo]
    m = librosa.feature.melspectrogram(y=a[:-160], sr=16000, n_fft=321, hop_length=160, n_mels=120)
    m = (librosa.power_to_db(m, ref=np.max) + 40) / 40
    return float(p808.run(None, {"input_1": m.T[None, :].astype(np.float32)})[0][0][0])
