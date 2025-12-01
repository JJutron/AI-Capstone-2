<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from "vue";
import { useRouter } from "vue-router";
import { useSkinStore } from "@/stores/skinStore";
import { FaceMesh } from "@mediapipe/face_mesh";
import { Camera } from "@mediapipe/camera_utils";

const faceMesh = new FaceMesh({
  locateFile: (file: string) =>
    `https://cdn.jsdelivr.net/npm/@mediapipe/face_mesh/${file}`,
});

faceMesh.setOptions({
  maxNumFaces: 1,
  refineLandmarks: true,
  minDetectionConfidence: 0.6,
  minTrackingConfidence: 0.6,
});

let mediapipeCamera: any = null;

const router = useRouter();
const skinStore = useSkinStore();

const video = ref<HTMLVideoElement | null>(null);
const canvas = ref<HTMLCanvasElement | null>(null);

const insideGuide = ref(false);
const countdown = ref(0);

let detector: any = null;
let animationId: number | null = null;
let stream: MediaStream | null = null;
let countdownTimer: any = null;

const GUIDE_W = 240;
const GUIDE_H = 310;

const loadDetector = async () => {
  const w = window as any;
  detector = w.FaceDetector ? new w.FaceDetector({ fastMode: true, maxDetectedFaces: 1 }) : null;
};
const checkInsideEllipse = (x: number, y: number, cx: number, cy: number, rx: number, ry: number) => {
  return ((x - cx) ** 2) / (rx ** 2) + ((y - cy) ** 2) / (ry ** 2) <= 1;
};

const checkFace = async () => {
  if (!video.value) return requestAnimationFrame(checkFace);

  try {
    const faces = detector ? await detector.detect(video.value) : [];
    if (!faces.length) {
      insideGuide.value = false;
    } else {
      const box = faces[0].boundingBox;

      // 1️⃣ 원본 크기
      const vw = video.value.videoWidth;
      const vh = video.value.videoHeight;

      // 2️⃣ 실제 표시 크기
      const dispW = video.value.clientWidth;
      const dispH = video.value.clientHeight;

      // 3️⃣ 스케일 전환 (원본 → 화면크기 변환 비율)
      const scaleX = dispW / vw;
      const scaleY = dispH / vh;

      // 4️⃣ 기존 박스를 화면 스케일에 맞게 변환
      const faceX = box.x * scaleX;
      const faceY = box.y * scaleY;
      const faceW = box.width * scaleX;
      const faceH = box.height * scaleY;

      const cx = faceX + faceW / 2;
      const cy = faceY + faceH / 2;

      // 5️⃣ 가이드 좌표 (이미 화면 기준임)
      const guideX = dispW / 2;
      const guideY = dispH / 2;
      const rx = GUIDE_W / 2;
      const ry = GUIDE_H / 2;

      // 6️⃣ 중심 기반 타원 공식
      const inEllipse =
        ((cx - guideX) ** 2) / (rx ** 2) +
        ((cy - guideY) ** 2) / (ry ** 2) <= 1;

      // 7️⃣ 얼굴 크기 조건 (너무 크거나 작지 않게)
      const sizeOK =
        faceW > rx * 0.7 &&
        faceH > ry * 0.7 &&
        faceW < rx * 1.5 &&
        faceH < ry * 1.5;

      insideGuide.value = inEllipse && sizeOK;
    }
  } catch {
    insideGuide.value = false;
  }

  animationId = requestAnimationFrame(checkFace);
};




const canShoot = computed(() => insideGuide.value);

const startCountdown = () => {
  if (countdownTimer || !canShoot.value) return;
  countdown.value = 3;

  countdownTimer = setInterval(() => {
    if (!canShoot.value) {
      cancelCountdown();
      return;
    }
    countdown.value -= 1;
    if (countdown.value <= 0) {
      clearInterval(countdownTimer);
      countdownTimer = null;
      capture();
    }
  }, 1000);
};

const cancelCountdown = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer);
    countdownTimer = null;
    countdown.value = 0;
  }
};

const handleCaptureBtn = () => {
  if (canShoot.value) startCountdown();
};

const capture = async () => {
  if (!video.value || !canvas.value) return;

  const v = video.value;
  const c = canvas.value;
  const ctx = c.getContext("2d")!;

  c.width = v.videoWidth;
  c.height = v.videoHeight;

  ctx.drawImage(v, 0, 0, c.width, c.height);

  const blob = await new Promise<Blob | null>(resolve =>
    c.toBlob(b => resolve(b), "image/png")
  );

  if (!blob) return alert("이미지 생성 실패");

  const file = new File([blob], "face.png", { type: "image/png" });

  skinStore.setFaceImage(file);
  router.push("/survey");
};
const essentialLandmarks = [1, 13, 468, 473]; // nose, mouth, left eye, right eye

const checkMesh = (landmarks: any[]) => {
  if (!landmarks || !landmarks.length) {
    insideGuide.value = false;
    return;
  }

  const w = video.value!.clientWidth;
  const h = video.value!.clientHeight;

  // 타원 기준
  const guideX = w / 2;
  const guideY = h / 2;
  const rx = GUIDE_W / 2;
  const ry = GUIDE_H / 2;

  // 타원 체크 함수
  const insideEllipse = (x: number, y: number) =>
    ((x - guideX) ** 2) / rx ** 2 + ((y - guideY) ** 2) / ry ** 2 < 1;

  // 핵심 landmark 최소 3개 이상 들어오면 OK
  const insideCount = essentialLandmarks.filter((idx) => {
    const px = landmarks[idx].x * w;
    const py = landmarks[idx].y * h;
    return insideEllipse(px, py);
  }).length;

  insideGuide.value = insideCount >= 3;
};

onMounted(async () => {
  stream = await navigator.mediaDevices.getUserMedia({ video: true });
  video.value!.srcObject = stream;

  faceMesh.onResults((res: any) => {
    if (res.multiFaceLandmarks) {
      checkMesh(res.multiFaceLandmarks[0]);
    } else insideGuide.value = false;
  });

  mediapipeCamera = new Camera(video.value!, {
    onFrame: async () => await faceMesh.send({ image: video.value }),
  });

  mediapipeCamera.start();
});


onBeforeUnmount(() => {
  cancelCountdown();
  if (animationId) cancelAnimationFrame(animationId);
  if (stream) stream.getTracks().forEach(t => t.stop());
});
</script>

<template>
  <div class="camera-root">
    <div class="camera">
      <video ref="video" autoplay playsinline class="video" />
      <canvas ref="canvas" class="canvas"></canvas>

      <p class="guide-text" :class="{ ok: insideGuide }">
        {{ insideGuide ? "정확합니다! 촬영을 시작합니다." : "얼굴을 가이드 안에 맞춰주세요" }}
      </p>

      <div class="oval" :class="{ ok: insideGuide }"></div>

      <div v-if="countdown > 0" class="countdown">{{ countdown }}</div>

      <button class="shoot" :disabled="!canShoot" @click="handleCaptureBtn">
        촬영하기
      </button>
    </div>
  </div>
</template>

<style scoped>
.camera-root {
  width: 100%;
  min-height: 100vh;
  background: #fff;
  display: flex;
  justify-content: center;
  align-items: center;
}

.camera {
  width: 393px;
  height: 852px;
  background: #000;
  position: relative;
  overflow: hidden;
}

.video,
.canvas {
  width: 100%;
  height: 100%;
  object-fit: cover;
  position: absolute;
}

.guide-text {
  position: absolute;
  top: 26px;
  width: 100%;
  text-align: center;
  color: #ff6b6b;
  font-size: 14px;
  transition: color 0.2s;
}

.guide-text.ok {
  color: #27481e;
  font-weight: bold;
}

.oval {
  position: absolute;
  width: 240px;
  height: 310px;
  border-radius: 46% 46% 50% 50%;
  border: 3px solid rgba(255, 0, 0, 0.7);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  transition: border-color 0.25s;
}

.oval.ok {
  border-color: #27481e;
}

.countdown {
  position: absolute;
  top: 44%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
  font-size: 120px;
  font-weight: 800;
}

.shoot {
  position: absolute;
  bottom: 42px;
  left: 50%;
  transform: translateX(-50%);
  width: 220px;
  height: 52px;
  border-radius: 14px;
  background: #27481e;
  color: white;
  font-size: 18px;
  cursor: pointer;
}

.shoot:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
