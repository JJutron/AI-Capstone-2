<template>
  <div class="join">
    <img class="back" :src="back" @click="goBack" />

    <div class="title">Email로 가입하기</div>

    <div class="form">
      <label class="label">이메일</label>
      <div class="box">
        <input type="email" v-model="email" placeholder="이메일을 입력하세요" />
      </div>

      <label class="label">닉네임</label>
      <div class="box">
        <input v-model="nickname" placeholder="닉네임을 입력하세요" />
      </div>

      <label class="label">생년월일</label>
      <div class="box">
        <input type="date" v-model="birthDate" />
      </div>

      <label class="label">성별</label>
      <div class="box">
        <select v-model="gender">
          <option value="">성별 선택</option>
          <option value="F">여성</option>
          <option value="M">남성</option>
        </select>
      </div>

      <label class="label">비밀번호</label>
      <div class="box password-box">
        <input
          :type="showPassword ? 'text' : 'password'"
          v-model="password"
          placeholder="영문, 숫자, 특수기호 조합을 권장드려요"
        />
        <img
          class="eye"
          :src="showPassword ? eyeClose : eyeOpen"
          @click="togglePassword"
        />
      </div>

      <button class="join-btn" @click="handleSignup" :disabled="loading">
        {{ loading ? "가입 중..." : "VE:GIN 시작하기" }}
      </button>
    </div>

    <p class="bottom-text">
      이미 계정이 있으신가요?
      <span class="link" @click="goLogin">로그인하기</span>
    </p>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { signupAPI } from "@/api/axios";
import { useUserStore } from "@/stores/userStore";

const router = useRouter();
const userStore = useUserStore();

const email = ref("");
const nickname = ref("");
const birthDate = ref("");
const gender = ref("");
const password = ref("");
const loading = ref(false);
const showPassword = ref(false);

const togglePassword = () => (showPassword.value = !showPassword.value);

const handleSignup = async () => {
  if (!email.value || !password.value || !birthDate.value || !gender.value) {
    alert("모든 필수 항목을 입력해주세요.");
    return;
  }

  loading.value = true;
  try {
    const res = await signupAPI({
      email: email.value,
      password: password.value,
      nickname: nickname.value,
      birthDate: birthDate.value,
      gender: gender.value === "F" ? "FEMALE" : "MALE",
    });

    alert("회원가입 완료!");
    router.push("/login");
  } catch (err: any) {
    alert(err.response?.data?.message || "회원가입 실패");
  } finally {
    loading.value = false;
  }
};

const goLogin = () => router.push("/login");
const goBack = () => router.push("/");
</script>


<style scoped>
/* 기존 스타일 그대로 유지 */
.join {
  width: 393px;
  height: 852px;
  background: #ffffff;
  margin: 0 auto;
  position: relative;
  padding: 40px 24px 24px;
  box-sizing: border-box;
}
.back {
  width: 30px;
  height: 30px;
  object-fit: contain;
  image-rendering: crisp-edges;  
  cursor: pointer;
  margin-bottom: 26px;
  margin-bottom: 26px;
}

.title {
  font-size: 22px;
  font-weight: 600;
  color: #27481e;
  margin-bottom: 26px;
  text-align: center;
}

.eye {
  position: absolute;
  right: 18px;
  width: 22px;
  height: 22px;
  cursor: pointer;
}

.label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  color: #27481e;
}
.box {
  width: 100%;
  height: 54px;
  background: #f3f4f6;
  border-radius: 10px;
  border: 1px solid #e5e5e5;
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  padding: 0 18px;
}
.box input,
.box select {
  width: 100%;
  height: 100%;
  border: none;
  outline: none;
  background: none;
  font-size: 15px;
  color: #27481e;
}
.password-box {
  position: relative;
}

.join-btn {
  width: 100%;
  height: 56px;
  margin-top: 4px;
  border: none;
  background: #27481e;
  color: white;
  border-radius: 12px;
  font-size: 18px;
  cursor: pointer;
}
.join-btn:disabled {
  opacity: 0.7;
}
.bottom-text {
  position: absolute;
  bottom: 32px;
  left: 0;
  right: 0;
  text-align: center;
  font-size: 13px;
  color: #7b7b7b;
}
.link {
  color: #27481e;
  cursor: pointer;
  text-decoration: underline;
}
</style>
