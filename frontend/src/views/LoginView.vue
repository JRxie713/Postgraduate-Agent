<script setup>
import { reactive, ref, onMounted, onBeforeUnmount } from 'vue'
import {
  getCaptcha,
  login,
  register,
  getLoginStatus,
  sendSmsCode,
  loginBySms,
  loginByEmail,
  sendEmailCode
} from '../api/auth'
import { TOKEN_KEY, REFRESH_KEY } from '../api/http'

const emit = defineEmits(['success'])

const mode = ref('password')
const form = reactive({
  userName: localStorage.getItem('lastUserName') || '',
  pwd: '',
  confirmPwd: '',
  captchaCode: '',
  phone: localStorage.getItem('lastPhone') || '',
  email: localStorage.getItem('lastEmail') || '',
  smsCode: '',
  emailCode: ''
})
const bindDialog = reactive({ visible: false, phone: '', email: '' })
const captcha = reactive({ id: '', image: '' })
const remember = ref(true)
const submitting = ref(false)
const error = ref('')
const notice = ref('')
const lock = reactive({ locked: false, remainSeconds: 0, remainingAttempts: 5, maxFail: 5 })
const sms = reactive({ sending: false, cooldown: 0 })
const email = reactive({ sending: false, cooldown: 0 })

let lockTimer = null
let smsTimer = null
let emailTimer = null

async function loadCaptcha() {
  const data = await getCaptcha()
  if (data.code === 200 && data.data) {
    captcha.id = data.data.captchaId
    captcha.image = data.data.image
    form.captchaCode = ''
  } else {
    error.value = data.message || '验证码加载失败'
  }
}

function stopLockCountdown() {
  if (lockTimer) {
    clearInterval(lockTimer)
    lockTimer = null
  }
}

function startLockCountdown() {
  stopLockCountdown()
  lockTimer = setInterval(async () => {
    if (lock.remainSeconds > 1) {
      lock.remainSeconds -= 1
      return
    }
    stopLockCountdown()
    lock.locked = false
    lock.remainSeconds = 0
    notice.value = '锁定已自动解除，可以重新登录'
    await checkStatus()
    await loadCaptcha()
  }, 1000)
}

function startSmsCooldown(seconds) {
  if (smsTimer) clearInterval(smsTimer)
  sms.cooldown = seconds
  smsTimer = setInterval(() => {
    sms.cooldown -= 1
    if (sms.cooldown <= 0) {
      clearInterval(smsTimer)
      smsTimer = null
      sms.cooldown = 0
    }
  }, 1000)
}

function startEmailCooldown(seconds) {
  if (emailTimer) clearInterval(emailTimer)
  email.cooldown = seconds
  emailTimer = setInterval(() => {
    email.cooldown -= 1
    if (email.cooldown <= 0) {
      clearInterval(emailTimer)
      emailTimer = null
      email.cooldown = 0
    }
  }, 1000)
}

async function checkStatus() {
  const name = form.userName.trim()
  if (!name) return
  const data = await getLoginStatus(name)
  if (data.code === 200 && data.data) {
    lock.locked = data.data.locked
    lock.remainSeconds = data.data.remainSeconds
    lock.remainingAttempts = data.data.remainingAttempts
    lock.maxFail = data.data.maxFail
    if (lock.locked) {
      error.value = ''
      startLockCountdown()
    }
  }
}

function switchMode(next) {
  if (submitting.value || sms.sending || email.sending) return
  mode.value = next
  error.value = ''
  notice.value = ''
  bindDialog.visible = false
  form.pwd = ''
  form.confirmPwd = ''
  form.smsCode = ''
  form.emailCode = ''
  form.captchaCode = ''
  loadCaptcha()
}

function validate() {
  if (mode.value === 'sms') {
    if (!/^1[3-9]\d{9}$/.test(form.phone.trim())) return '请输入正确的手机号'
    if (!/^\d{6}$/.test(form.smsCode.trim())) return '请输入 6 位短信验证码'
    return ''
  }
  if (mode.value === 'email') {
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) return '请输入正确的邮箱'
    if (!/^\d{6}$/.test(form.emailCode.trim())) return '请输入 6 位邮箱验证码'
    return ''
  }
  if (!form.userName.trim()) return '请输入账号'
  if (form.pwd.length < 6) return '密码长度不能少于 6 位'
  if (mode.value === 'register' && form.captchaCode.trim().length !== 4) return '请输入 4 位图形验证码'
  if (mode.value === 'register' && form.pwd !== form.confirmPwd) return '两次输入的密码不一致'
  return ''
}

async function sendSms() {
  if (sms.sending || sms.cooldown > 0) return
  error.value = ''
  notice.value = ''
  if (!/^1[3-9]\d{9}$/.test(form.phone.trim())) {
    error.value = '请输入正确的手机号'
    return
  }
  if (form.captchaCode.trim().length !== 4) {
    error.value = '请先输入 4 位图形验证码'
    return
  }
  sms.sending = true
  try {
    const data = await sendSmsCode({
      phone: form.phone.trim(),
      captchaId: captcha.id,
      captchaCode: form.captchaCode.trim()
    })
    if (data.code === 200 && data.data) {
      notice.value = '短信验证码已发送，5 分钟内有效'
      startSmsCooldown(data.data.cooldown || 60)
      // 图形验证码已一次性消费，发送后立即刷新
      await loadCaptcha()
    } else {
      error.value = data.message || '验证码发送失败'
      await loadCaptcha()
    }
  } finally {
    sms.sending = false
  }
}

async function sendEmail() {
  if (email.sending || email.cooldown > 0) return
  error.value = ''
  notice.value = ''
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
    error.value = '请输入正确的邮箱'
    return
  }
  if (form.captchaCode.trim().length !== 4) {
    error.value = '请先输入 4 位图形验证码'
    return
  }
  email.sending = true
  try {
    const data = await sendEmailCode({
      email: form.email.trim(),
      captchaId: captcha.id,
      captchaCode: form.captchaCode.trim()
    })
    if (data.code === 200 && data.data) {
      notice.value = data.data.delivered
        ? '验证码已发送到该邮箱，请查收（含垃圾邮件箱），5 分钟内有效'
        : '验证码已生成，但后端未配置邮箱授权码，暂未真实发信（详见后端日志）'
      startEmailCooldown(data.data.cooldown || 60)
      // 图形验证码已一次性消费，发送后立即刷新
      await loadCaptcha()
    } else {
      error.value = data.message || '验证码发送失败'
      await loadCaptcha()
    }
  } finally {
    email.sending = false
  }
}

function finishLogin(data, name) {
  localStorage.setItem(TOKEN_KEY, data.accessToken)
  localStorage.setItem(REFRESH_KEY, data.refreshToken)
  if (name) {
    if (remember.value) {
      localStorage.setItem('lastUserName', name)
    } else {
      localStorage.removeItem('lastUserName')
    }
  }
  if (form.phone.trim()) {
    localStorage.setItem('lastPhone', form.phone.trim())
  }
  emit('success')
}

async function submit() {
  if (submitting.value) return
  error.value = ''
  notice.value = ''
  const invalid = validate()
  if (invalid) {
    error.value = invalid
    return
  }
  if (mode.value === 'password' && lock.locked) return

  submitting.value = true
  try {
    if (mode.value === 'register') {
      // 账号密码和图形验证码通过后，再弹出绑定手机号 / 邮箱的选择
      bindDialog.phone = ''
      bindDialog.email = ''
      bindDialog.visible = true
      return
    }

    if (mode.value === 'email') {
      const data = await loginByEmail({
        email: form.email.trim(),
        emailCode: form.emailCode.trim()
      })
      if (data.code === 200 && data.data) {
        localStorage.setItem('lastEmail', form.email.trim())
        finishLogin(data.data)
        return
      }
      error.value = data.message || '登录失败'
      return
    }

    if (mode.value === 'sms') {
      const data = await loginBySms({
        phone: form.phone.trim(),
        smsCode: form.smsCode.trim()
      })
      if (data.code === 200 && data.data) {
        finishLogin(data.data, form.phone.trim())
        return
      }
      error.value = data.message || '登录失败'
      return
    }

    const data = await login({
      userName: form.userName.trim(),
      pwd: form.pwd
    })
    if (data.code === 200 && data.data) {
      finishLogin(data.data, form.userName.trim())
      return
    }
    error.value = data.message || '登录失败'
    if (data.code === 423) {
      await checkStatus()
    }
  } finally {
    submitting.value = false
  }
}

/** 提交注册：skip=true 表示暂不绑定任何联系方式 */
async function doRegister(skip) {
  if (submitting.value) return
  error.value = ''
  notice.value = ''
  const phone = skip ? '' : bindDialog.phone.trim()
  const email = skip ? '' : bindDialog.email.trim()
  if (phone && !/^1[3-9]\d{9}$/.test(phone)) {
    error.value = '请输入正确的 11 位手机号'
    return
  }
  if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    error.value = '请输入正确的邮箱'
    return
  }
  submitting.value = true
  try {
    const data = await register({
      userName: form.userName.trim(),
      pwd: form.pwd,
      phone,
      email,
      captchaId: captcha.id,
      captchaCode: form.captchaCode.trim()
    })
    if (data.code !== 200) {
      error.value = data.message || '注册失败'
      bindDialog.visible = false
      await loadCaptcha()
      return
    }
    bindDialog.visible = false
    const bound = []
    if (phone) bound.push('手机号')
    if (email) bound.push('邮箱')
    notice.value = bound.length
      ? `注册成功，已绑定${bound.join('和')}，可用账号密码或对应方式登录`
      : '注册成功，可使用账号密码登录'
    mode.value = 'password'
    form.pwd = ''
    form.confirmPwd = ''
    await loadCaptcha()
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadCaptcha()
  if (form.userName && mode.value === 'password') {
    await checkStatus()
  }
})

onBeforeUnmount(() => {
  stopLockCountdown()
  if (smsTimer) clearInterval(smsTimer)
  if (emailTimer) clearInterval(emailTimer)
})
</script>

<template>
  <div class="login-page">
    <div class="glow glow-a"></div>
    <div class="glow glow-b"></div>

    <div class="login-shell">
      <section class="brand">
        <div class="logo">研</div>
        <h1>考研 AI 导学系统</h1>
        <p class="brand-desc">学习计划、错题复盘、模考诊断与 AI 答疑，一站式备考工作台。</p>
      </section>

      <section class="panel">
        <div class="tabs">
          <button :class="['tab', { active: mode === 'password' }]" type="button" @click="switchMode('password')">密码登录</button>
          <button :class="['tab', { active: mode === 'email' }]" type="button" @click="switchMode('email')">邮箱登录</button>
          <button :class="['tab', { active: mode === 'sms' }]" type="button" @click="switchMode('sms')">手机号登录</button>
          <button :class="['tab', { active: mode === 'register' }]" type="button" @click="switchMode('register')">注册</button>
        </div>

        <form class="form" @submit.prevent="submit">
          <!-- 密码登录 -->
          <template v-if="mode === 'password'">
            <label class="field">
              <span>账号</span>
              <input v-model="form.userName" type="text" autocomplete="username" placeholder="请输入登录账号" @blur="checkStatus" />
            </label>
            <label class="field">
              <span>密码</span>
              <input v-model="form.pwd" type="password" autocomplete="current-password" placeholder="至少 6 位" />
            </label>
          </template>

          <!-- 邮箱登录 -->
          <template v-if="mode === 'email'">
            <label class="field">
              <span>邮箱</span>
              <input v-model="form.email" type="email" autocomplete="email" placeholder="请输入注册时绑定的邮箱" />
            </label>
            <div class="field">
              <span>邮箱验证码</span>
              <div class="sms-row">
                <input v-model="form.emailCode" type="text" maxlength="6" inputmode="numeric" placeholder="6 位数字验证码" />
                <button type="button" class="send-btn" :disabled="email.sending || email.cooldown > 0" @click="sendEmail">
                  {{ email.cooldown > 0 ? `${email.cooldown}s 后重发` : email.sending ? '发送中…' : '发送验证码' }}
                </button>
              </div>
            </div>
          </template>

          <!-- 手机号登录 -->
          <template v-if="mode === 'sms'">
            <label class="field">
              <span>手机号</span>
              <input v-model="form.phone" type="tel" maxlength="11" autocomplete="tel" placeholder="请输入注册时绑定的手机号" />
            </label>
            <div class="field">
              <span>短信验证码</span>
              <div class="sms-row">
                <input v-model="form.smsCode" type="text" maxlength="6" inputmode="numeric" placeholder="6 位数字验证码" />
                <button type="button" class="send-btn" :disabled="sms.sending || sms.cooldown > 0" @click="sendSms">
                  {{ sms.cooldown > 0 ? `${sms.cooldown}s 后重发` : sms.sending ? '发送中…' : '发送验证码' }}
                </button>
              </div>
            </div>
          </template>

          <!-- 注册（第一步：账号与密码） -->
          <template v-if="mode === 'register'">
            <label class="field">
              <span>账号</span>
              <input v-model="form.userName" type="text" autocomplete="username" placeholder="请输入登录账号" />
            </label>
            <label class="field">
              <span>密码</span>
              <input v-model="form.pwd" type="password" autocomplete="new-password" placeholder="至少 6 位" />
            </label>
            <label class="field">
              <span>确认密码</span>
              <input v-model="form.confirmPwd" type="password" autocomplete="new-password" placeholder="请再次输入密码" />
            </label>
            <p class="role-tip">公开注册仅创建学生账号；教师 / 管理员账号需由管理员在后台创建。</p>
          </template>

          <!-- 图形验证码：密码登录不再需要，注册直接校验；邮箱/手机号登录在发送验证码前校验 -->
          <div v-if="mode === 'register' || mode === 'sms' || mode === 'email'" class="field">
            <span>{{ mode === 'register' ? '图形验证码' : '图形验证码（发送验证码前校验）' }}</span>
            <div class="captcha-row">
              <input v-model="form.captchaCode" type="text" maxlength="4" placeholder="4 位验证码" />
              <button type="button" class="captcha-img" title="点击刷新验证码" @click="loadCaptcha">
                <img v-if="captcha.image" :src="captcha.image" alt="验证码" />
                <span v-else>刷新</span>
              </button>
            </div>
          </div>

          <label v-if="mode === 'password'" class="remember">
            <input v-model="remember" type="checkbox" />
            <span>记住账号</span>
            <span v-if="!lock.locked" class="attempts">剩余尝试 {{ lock.remainingAttempts }} / {{ lock.maxFail }} 次</span>
          </label>

          <p v-if="mode === 'password' && lock.locked" class="alert warn">
            账号已锁定，{{ lock.remainSeconds }} 秒后自动解锁
          </p>
          <p v-else-if="error" class="alert error">{{ error }}</p>
          <p v-else-if="notice" class="alert ok">{{ notice }}</p>

          <button class="submit" type="submit" :disabled="submitting || (mode === 'password' && lock.locked)">
            <span v-if="submitting" class="spinner"></span>
            {{
              mode === 'password' && lock.locked
                ? '账号已锁定'
                : submitting
                  ? '处理中…'
                  : mode === 'register'
                    ? '注 册'
                    : '登 录'
            }}
          </button>

          <p class="tip">
            {{ mode === 'register' ? '已有账号？' : '还没有账号？' }}
            <a href="#" @click.prevent="switchMode(mode === 'register' ? 'password' : 'register')">
              {{ mode === 'register' ? '返回登录' : '立即注册' }}
            </a>
          </p>
        </form>
      </section>
    </div>

    <!-- 注册第二步：选择绑定手机号 / 邮箱 -->
    <div v-if="bindDialog.visible" class="modal-mask">
      <div class="modal">
        <h3>绑定手机号或邮箱</h3>
        <p class="modal-desc">
          即将创建学生账号。绑定后可用手机号验证码或邮箱验证码快捷登录，两项可以只绑定一个，也可以暂不绑定。
        </p>
        <label class="field">
          <span>手机号（选填）</span>
          <input v-model="bindDialog.phone" type="tel" maxlength="11" placeholder="11 位手机号" />
        </label>
        <label class="field">
          <span>邮箱（选填）</span>
          <input v-model="bindDialog.email" type="email" placeholder="example@qq.com" />
        </label>
        <p v-if="error" class="alert error">{{ error }}</p>
        <div class="modal-actions">
          <button type="button" class="ghost" :disabled="submitting" @click="doRegister(true)">暂不绑定</button>
          <button type="button" class="primary" :disabled="submitting" @click="doRegister(false)">保存并完成注册</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 20px;
  overflow: hidden;
  background: #f4f7fb;
}
.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.55;
  pointer-events: none;
}
.glow-a {
  width: 420px;
  height: 420px;
  background: #cfe0ff;
  top: -120px;
  left: -80px;
}
.glow-b {
  width: 380px;
  height: 380px;
  background: #d8f3e6;
  bottom: -140px;
  right: -60px;
}
.login-shell {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 920px;
  display: grid;
  grid-template-columns: 1fr 400px;
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 24px 60px rgba(15, 35, 70, 0.12);
  overflow: hidden;
}
.brand {
  padding: 48px 40px;
  color: #eaf2ff;
  background: linear-gradient(160deg, #1c3f7a 0%, #14294f 60%, #0f1f3d 100%);
  display: flex;
  flex-direction: column;
}
.logo {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-size: 22px;
  font-weight: 700;
  color: #14294f;
  background: #7fb4ff;
}
.brand h1 {
  margin: 22px 0 0;
  font-size: 26px;
  line-height: 1.35;
  color: #ffffff;
}
.brand-desc {
  margin: 14px 0 0;
  font-size: 14px;
  line-height: 1.8;
  color: #a9bfe0;
}
.panel {
  padding: 40px 38px;
}
.tabs {
  display: flex;
  gap: 14px;
  margin-bottom: 24px;
  border-bottom: 1px solid #e8edf5;
}
.tab {
  padding: 0 2px 12px;
  border: none;
  background: none;
  font-size: 13px;
  color: #8a94a6;
  cursor: pointer;
  white-space: nowrap;
  border-bottom: 2px solid transparent;
}
.tab.active {
  color: #1c3f7a;
  font-weight: 600;
  border-bottom-color: #2f6fe4;
}
.form {
  display: grid;
  gap: 16px;
}
.field {
  display: grid;
  gap: 7px;
  font-size: 13px;
  color: #57627a;
}
.field input {
  width: 100%;
  box-sizing: border-box;
  padding: 11px 12px;
  font-size: 14px;
  color: #1b2434;
  background: #f7f9fc;
  border: 1px solid #dde4ee;
  border-radius: 10px;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.field input:focus {
  border-color: #2f6fe4;
  box-shadow: 0 0 0 3px rgba(47, 111, 228, 0.12);
  background: #ffffff;
}
.captcha-row {
  display: grid;
  grid-template-columns: 1fr 120px;
  gap: 10px;
}
.captcha-img {
  height: 42px;
  padding: 0;
  overflow: hidden;
  border: 1px solid #dde4ee;
  border-radius: 10px;
  background: #f7f9fc;
  cursor: pointer;
}
.captcha-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.captcha-img span {
  font-size: 13px;
  color: #8a94a6;
}
.sms-row {
  display: grid;
  grid-template-columns: 1fr 128px;
  gap: 10px;
}
.send-btn {
  height: 42px;
  padding: 0 10px;
  border: 1px solid #c9d9f7;
  border-radius: 10px;
  background: #eef4ff;
  color: #1c4fb8;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
}
.send-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.remember {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #7c8698;
}
.remember input {
  accent-color: #2f6fe4;
}
.attempts {
  margin-left: auto;
}
.alert {
  margin: 0;
  padding: 9px 12px;
  border-radius: 9px;
  font-size: 13px;
  line-height: 1.5;
}
.alert.error {
  color: #b42318;
  background: #fef3f2;
  border: 1px solid #fecdca;
}
.alert.warn {
  color: #93370d;
  background: #fffaeb;
  border: 1px solid #fedf89;
}
.alert.ok {
  color: #067647;
  background: #ecfdf3;
  border: 1px solid #abefc6;
}
.role-tip {
  margin: 0;
  padding: 8px 12px;
  border-radius: 9px;
  background: #f4f7fb;
  color: #6b7688;
  font-size: 12px;
  line-height: 1.6;
}
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 10;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(15, 31, 61, 0.45);
}
.modal {
  width: 100%;
  max-width: 380px;
  padding: 26px 26px 22px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 24px 60px rgba(15, 35, 70, 0.24);
  display: grid;
  gap: 14px;
}
.modal h3 {
  margin: 0;
  font-size: 18px;
  color: #16233a;
}
.modal-desc {
  margin: 0;
  font-size: 12px;
  line-height: 1.7;
  color: #7a869a;
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 4px;
}
.modal-actions button {
  padding: 9px 16px;
  border-radius: 9px;
  font-size: 13px;
  cursor: pointer;
}
.modal-actions .ghost {
  border: 1px solid #dde4ee;
  background: #ffffff;
  color: #46526b;
}
.modal-actions .primary {
  border: none;
  background: linear-gradient(135deg, #2f6fe4, #1c4fb8);
  color: #ffffff;
}
.modal-actions button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.submit {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 44px;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 2px;
  color: #ffffff;
  background: linear-gradient(135deg, #2f6fe4, #1c4fb8);
  cursor: pointer;
  transition: opacity 0.15s, transform 0.15s;
}
.submit:hover:not(:disabled) {
  transform: translateY(-1px);
}
.submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
.tip {
  margin: 0;
  text-align: center;
  font-size: 12px;
  color: #8a94a6;
}
.tip a {
  color: #2f6fe4;
  text-decoration: none;
}
@media (max-width: 820px) {
  .login-shell {
    grid-template-columns: 1fr;
    max-width: 420px;
  }
  .brand {
    padding: 28px 26px;
  }
  .brand h1 {
    font-size: 21px;
    margin-top: 14px;
  }
  .panel {
    padding: 28px 24px 32px;
  }
}
</style>
