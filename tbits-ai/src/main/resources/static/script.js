/* DOM Elements */
const chatInner = document.getElementById('chat-inner');
const emptyState = document.getElementById('empty-state');
const typingWrap = document.getElementById('typing-wrap');
const liveTimer = document.getElementById('live-timer');
const sendBtn = document.getElementById('send-btn');
const chatCont = document.getElementById('chat-container');

let isBusy = false;
let timerInterval = null;
let t0 = 0;

/* Theme Setup */
function toggleTheme(checkbox) {
  const isDark = checkbox.checked;
  document.documentElement.setAttribute('data-theme', isDark ? 'dark' : 'light');
  localStorage.setItem('tbits-theme', isDark ? 'dark' : 'light');
  updateThemeUI(isDark);
}

function updateThemeUI(isDark) {
  const label = document.getElementById('theme-label-text');
  label.textContent = isDark ? 'Dark mode' : 'Light mode';
}

(function initTheme() {
  const saved = localStorage.getItem('tbits-theme') || 'light';
  const isDark = saved === 'dark';
  document.documentElement.setAttribute('data-theme', saved);
  document.getElementById('theme-checkbox').checked = isDark;
  updateThemeUI(isDark);
})();

/* UI Helpers */
function autoResize(el) {
  el.style.height = 'auto';
  el.style.height = Math.min(el.scrollHeight, 150) + 'px';
}

function handleKey(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    sendMessage();
  }
}

function scrollBottom() {
  chatCont.scrollTo({ top: chatCont.scrollHeight, behavior: 'smooth' });
}

function newChat() {
  chatInner.innerHTML = '';
  if (emptyState) emptyState.style.display = 'block';
}

/* Markdown Formatter */
function fmt(t) {
  return t.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
          .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
          .replace(/\*(.*?)\*/g, '<em>$1</em>')
          .replace(/`([^`]+)`/g, '<code>$1</code>')
          .replace(/\n/g, '<br>');
}

/* Add Message to UI */
function addMessage(text, role, elapsed) {
  if (emptyState) emptyState.style.display = 'none';

  const row = document.createElement('div');
  row.className = `msg-row ${role}`;

  let timerHTML = '';
  if (role === 'ai' && elapsed != null) {
    timerHTML = `<div class="timer-line">⏱ ${elapsed.toFixed(2)}s</div>`;
  }

  if (role === 'user') {
    row.innerHTML = `<div class="avatar user-avatar">U</div><div class="msg-content">${fmt(text)}</div>`;
  } else {
    row.innerHTML = `<div class="avatar ai-avatar">t</div><div class="msg-content"></div>${timerHTML}`;
  }

  chatInner.appendChild(row);
  scrollBottom();
  
  return role === 'ai' ? row.querySelector('.msg-content') : null;
}

/* Simulate Streaming Effect */
function simulateStream(bubbleEl, fullText, elapsed) {
  let i = 0;
  const cursor = document.createElement('span');
  cursor.className = 'streaming-cursor';
  bubbleEl.appendChild(cursor);

  function step() {
    if (i < fullText.length) {
      bubbleEl.innerHTML = fmt(fullText.slice(0, ++i));
      bubbleEl.appendChild(cursor);
      scrollBottom();
      const delay = (fullText.length - i) > 200 ? 5 : 15;
      setTimeout(step, delay);
    } else {
      cursor.remove();
      if (elapsed != null) {
        const timerEl = document.createElement('div');
        timerEl.className = 'timer-line';
        timerEl.innerHTML = `⏱ ${elapsed.toFixed(2)}s`;
        bubbleEl.parentElement.appendChild(timerEl);
      }
      scrollBottom();
      isBusy = false;
      document.getElementById('user-input').focus();
    }
  }
  step();
}

/* Timer */
function startTimer() {
  t0 = performance.now();
  timerInterval = setInterval(() => {
    liveTimer.textContent = ((performance.now() - t0) / 1000).toFixed(1) + 's';
  }, 100);
}

function stopTimer() {
  clearInterval(timerInterval);
  liveTimer.textContent = '';
  return (performance.now() - t0) / 1000;
}

/* 🚀 Send Message API Call */
async function sendMessage() {
  const inp = document.getElementById('user-input');
  const msg = inp.value.trim();

  if (!msg || isBusy) return;
  isBusy = true;
  
  inp.value = '';
  inp.style.height = 'auto';

  addMessage(msg, 'user');
  
  typingWrap.style.display = 'block';
  scrollBottom();
  startTimer();

  try {
    // RELATIVE PATH: Will automatically hit http://<Server-IP>:8080/tBits-AI/api/chat
    const res = await fetch('api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'text/plain' },
      body: msg
    });

    const elapsed = stopTimer();
    const fullText = await res.text();
    
    typingWrap.style.display = 'none';
    const bubbleEl = addMessage('', 'ai');
    simulateStream(bubbleEl, fullText, elapsed);

  } catch (err) {
    const elapsed = stopTimer();
    typingWrap.style.display = 'none';
    const bubbleEl = addMessage('', 'ai');
    simulateStream(bubbleEl, '🚨 Could not connect to the Java Backend. Ensure the .war file is deployed correctly.', elapsed);
  }
}