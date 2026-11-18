const app = document.querySelector('#app');
const labels = {
  role: {MANAGER:'руководитель',TESTER:'тестировщик',DEVELOPER:'разработчик',OBSERVER:'наблюдатель'},
  status: {NEW:'Новый',OPEN:'Открыт',IN_PROGRESS:'В работе',IN_REVIEW:'На проверке',RETURNED:'Возвращён',CLOSED:'Закрыт',REJECTED:'Отклонён'},
  severity: {BLOCKER:'Блокирующий',CRITICAL:'Критический',MAJOR:'Значительный',MINOR:'Незначительный'},
  priority: {URGENT:'Срочный',HIGH:'Высокий',NORMAL:'Обычный',LOW:'Низкий'}
};
let user = null;
let selectedProject = null;
let renderVersion = 0;
const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const date = value => new Intl.DateTimeFormat('ru-RU',{dateStyle:'short',timeStyle:'short',timeZone:'Europe/Moscow'}).format(new Date(value));
const options = (map, selected='', all=false) => (all?'<option value="">Все</option>':'') + Object.entries(map).map(([key,label])=>`<option value="${key}" ${key===selected?'selected':''}>${label}</option>`).join('');

class ApiError extends Error {
  constructor(status, data) { super(data.message || 'Не удалось выполнить действие.'); this.status=status; this.fields=data.fields || {}; }
}

async function api(path, {method='GET', body}={}) {
  let response;
  try {
    response = await fetch('/api'+path, {method, credentials:'same-origin', headers:{
      ...(body !== undefined ? {'Content-Type':'application/json'} : {}),
      ...(user && method !== 'GET' ? {'X-CSRF-Token':user.csrfToken} : {})
    }, ...(body !== undefined ? {body:JSON.stringify(body)} : {})});
  } catch {
    throw new ApiError(0,{message:method==='POST' && path.includes('/bugs')
      ? 'Связь потеряна. Баг мог сохраниться. Проверьте список перед повторной отправкой.'
      : 'Не удалось связаться с сервером. Проверьте подключение и повторите действие.'});
  }
  const data = response.status === 204 ? null : await response.json();
  if (!response.ok) throw new ApiError(response.status,data);
  return data;
}

function shell(content, screen='', current='') {
  app.setAttribute('aria-busy','false');
  app.innerHTML=`<div class="shell"><header class="header"><a class="brand" href="#projects">BugTrack</a>
    ${user?`<nav class="nav" aria-label="Основная навигация"><a href="#projects" ${current==='projects'?'aria-current="page"':''}>Мои проекты</a>${selectedProject?`<a href="#project/${selectedProject.id}" ${current==='bugs'?'aria-current="page"':''}>Баги</a>`:''}</nav><div class="account"><span>${esc(user.name)}</span><button type="button" class="logout" id="logout">Выйти</button></div>`:''}
    </header><main id="content" class="content" data-screen="${screen}" tabindex="-1">${content}</main></div>`;
  document.querySelector('#logout')?.addEventListener('click', async event=>{
    if (document.querySelector('#bug-form') && !confirm('Выйти? Черновик формы останется в этом браузере.')) return;
    event.target.disabled=true;
    try { await api('/auth/logout',{method:'POST'}); user=null; selectedProject=null; location.hash='login'; render(); }
    catch(error) { if(error.status===401){user=null;render();}else{showPageError(error.message);event.target.disabled=false;} }
  });
  document.querySelector('h1')?.setAttribute('tabindex','-1');
  document.querySelector('h1')?.focus({preventScroll:true});
  document.querySelector('form')?.addEventListener('input', event=>{
    const input=event.target;
    if(input.hasAttribute('aria-invalid')) {
      input.removeAttribute('aria-invalid');
      const error=document.getElementById(input.name+'-error');if(error)error.textContent='';
      if(!input.form.querySelector('[aria-invalid=true]')) {
        const message=input.form.querySelector('[role=alert]');if(message)message.textContent='';
      }
    }
  });
}

function showPageError(message) {
  let box=document.querySelector('#page-error');
  if(!box){box=document.createElement('p');box.id='page-error';box.className='message';box.setAttribute('role','alert');document.querySelector('main').prepend(box);}
  box.textContent=message;
}

function field(name,label,{type='text',value='',max=200,required=false,rows=false,items=null}={}) {
  const attr=`id="${name}" name="${name}" aria-describedby="${name}-error" ${required?'required':''}`;
  const control=items?`<select ${attr}>${options(items,value)}</select>`:rows
    ?`<textarea ${attr} maxlength="${max}" class="${name==='steps'?'steps':''}">${esc(value)}</textarea>`
    :`<input ${attr} type="${type}" maxlength="${max}" value="${esc(value)}" ${name==='password'?'autocomplete="current-password"':name==='email'?'autocomplete="username"':''}>`;
  return `<div class="field"><label for="${name}">${label}${required?' *':''}</label>${control}<p class="field-error" id="${name}-error"></p></div>`;
}

function formErrors(error, form) {
  form.querySelectorAll('.field-error').forEach(el=>el.textContent='');
  form.querySelectorAll('[aria-invalid]').forEach(el=>el.removeAttribute('aria-invalid'));
  for(const [name,message] of Object.entries(error.fields)) {
    const input=form.elements.namedItem(name);
    const target=document.getElementById(name+'-error');
    if(input && target){input.setAttribute('aria-invalid','true');target.textContent=message;}
  }
  const box=form.querySelector('[role=alert]'); box.textContent=error.message;
  (form.querySelector('[aria-invalid=true]') || box).focus();
}

function login(message='') {
  document.title='Вход | BugTrack';
  shell(`<h1>Вход</h1><form id="login-form" class="form" novalidate>
    ${field('email','Email',{type:'email',max:254})}${field('password','Пароль',{type:'password',max:128})}
    <p class="message" role="alert" tabindex="-1">${esc(message)}</p><div class="actions"><button class="btn" type="submit">Войти</button></div></form>`,'S01');
  document.querySelector('#login-form').addEventListener('submit',async event=>{
    event.preventDefault();const form=event.currentTarget,button=form.querySelector('button');button.disabled=true;button.textContent='Вход…';
    try {
      user=await api('/auth/login',{method:'POST',body:{email:form.email.value.trim(),password:form.password.value}});
      const target = location.hash && location.hash!=='#login' ? location.hash : '#projects';
      location.hash=target; await render();
    }catch(error){formErrors(error,form);button.disabled=false;button.textContent='Войти';}
  });
}

async function projects(version) {
  const data=await api('/projects'); if(version!==renderVersion)return;
  selectedProject=null;document.title='Мои проекты | BugTrack';
  shell(`<h1>Мои проекты</h1>${data.length?data.map(p=>`<section class="project" aria-label="${esc(p.name)}"><p>${esc(p.key)} - ${esc(p.name)}</p><p>Участники: ${p.members.map(m=>esc(m.name)+' - '+[...m.roles].sort().map(r=>labels.role[r]).join(', ')).join('; ')}.</p><div class="actions"><a class="btn" href="#project/${p.id}">Открыть ${esc(p.key)}</a></div></section>`).join(''):'<p class="empty">Вы пока не участвуете ни в одном проекте.</p>'}`,'S03','projects');
}

async function bugList(projectId, version) {
  const project=await api('/projects/'+projectId);if(version!==renderVersion)return;
  selectedProject=project;document.title=`Баги ${project.key} | BugTrack`;
  shell(`<h1>Баги проекта ${esc(project.key)}</h1><form id="filters" class="filters" role="search"><div class="field search"><label for="query">Поиск по номеру или заголовку</label><input id="query" name="query" maxlength="200" type="search"></div>
    <div class="field"><label for="status">Статус</label><select id="status" name="status">${options(labels.status,'',true)}</select></div>
    <div class="field"><label for="priority">Приоритет</label><select id="priority" name="priority">${options(labels.priority,'',true)}</select></div>
    <div class="actions"><button class="btn secondary" type="submit">Найти</button><button class="btn secondary" type="reset">Сбросить</button></div></form>
    <p id="list-status" role="status" aria-live="polite"></p><div class="table-wrap" id="bug-table"></div><div class="pagination" id="pagination"></div>
    ${project.canCreateBug?`<div class="actions"><a class="btn" href="#project/${project.id}/new">Новый баг</a></div>`:''}`,'S06','bugs');
  const filter=document.querySelector('#filters');let searchVersion=0;
  async function load(page=0){
    const n=++searchVersion;const status=document.querySelector('#list-status');status.textContent='Загрузка…';
    const params=new URLSearchParams(new FormData(filter));for(const [k,v] of [...params])if(!v)params.delete(k);params.set('page',page);
    try {
      const data=await api(`/projects/${projectId}/bugs?${params}`);if(version!==renderVersion||n!==searchVersion)return;
      status.textContent=`Найдено: ${data.total}`;
      document.querySelector('#bug-table').innerHTML=data.items.length?`<table><thead><tr><th>Номер</th><th>Заголовок</th><th>Статус</th><th>Приоритет</th><th>Исполнитель</th><th>Срок</th></tr></thead><tbody>${data.items.map(b=>`<tr><td><a href="#bug/${b.id}">${esc(b.code)}</a></td><td><a href="#bug/${b.id}">${esc(b.title)}</a></td><td>${labels.status[b.status]}</td><td>${labels.priority[b.priority]}</td><td>Не назначен</td><td>Не задан</td></tr>`).join('')}</tbody></table>`:'<p class="empty">Багов не найдено. Измените фильтры или создайте новый баг.</p>';
      const pagination=document.querySelector('#pagination');pagination.innerHTML=data.totalPages>1?`<button class="btn secondary" id="prev" ${page===0?'disabled':''}>Назад</button><span>Страница ${page+1} из ${data.totalPages}</span><button class="btn secondary" id="next" ${page+1>=data.totalPages?'disabled':''}>Далее</button>`:'';
      document.querySelector('#prev')?.addEventListener('click',()=>load(page-1));document.querySelector('#next')?.addEventListener('click',()=>load(page+1));
    }catch(error){if(version===renderVersion&&n===searchVersion){status.textContent='';handleError(error);}}
  }
  filter.addEventListener('submit',event=>{event.preventDefault();load();});
  filter.addEventListener('reset',()=>setTimeout(()=>load(),0));
  filter.status.addEventListener('change',()=>load());filter.priority.addEventListener('change',()=>load());
  await load();
}

async function newBug(projectId,version) {
  const project=await api('/projects/'+projectId);if(version!==renderVersion)return;
  if(!project.canCreateBug)throw new ApiError(403,{message:'Для создания бага недостаточно прав.'});
  selectedProject=project;document.title='Новый баг | BugTrack';
  const draftKey=`bugtrack.draft.${user.id}.${projectId}`;
  let draft={};try{draft=JSON.parse(sessionStorage.getItem(draftKey)||'{}');}catch{}
  shell(`<h1>Новый баг</h1><form id="bug-form" class="form" novalidate>
    ${field('title','Заголовок',{value:draft.title,required:true,max:200})}
    ${field('description','Описание',{value:draft.description,required:true,rows:true,max:10000})}
    ${field('steps','Шаги воспроизведения',{value:draft.steps,required:true,rows:true,max:10000})}
    ${field('expected','Ожидаемый результат',{value:draft.expected,required:true,max:5000})}
    ${field('actual','Фактический результат',{value:draft.actual,required:true,max:5000})}
    ${field('environment','Окружение',{value:draft.environment,max:2000})}
    ${field('severity','Серьёзность',{value:draft.severity||'MAJOR',required:true,items:labels.severity})}
    ${field('priority','Приоритет',{value:draft.priority||'NORMAL',required:true,items:labels.priority})}
    <p class="message" role="alert" tabindex="-1"></p><div class="actions"><button class="btn" type="submit">Создать</button><a class="btn secondary" id="cancel" href="#project/${projectId}">Отмена</a></div></form>`,'S07','bugs');
  const form=document.querySelector('#bug-form');
  const saveDraft=()=>{try{sessionStorage.setItem(draftKey,JSON.stringify(Object.fromEntries(new FormData(form))));}catch{}};
  form.addEventListener('input',saveDraft);form.addEventListener('change',saveDraft);
  document.querySelector('#cancel').addEventListener('click',event=>{
    if([...new FormData(form)].some(([k,v])=>!['severity','priority'].includes(k)&&v.trim())&&!confirm('Отменить создание и удалить черновик?')){event.preventDefault();return;}
    sessionStorage.removeItem(draftKey);
  });
  form.addEventListener('submit',async event=>{
    event.preventDefault();saveDraft();const button=form.querySelector('button[type=submit]');button.disabled=true;button.textContent='Сохранение…';
    try{const result=await api(`/projects/${projectId}/bugs`,{method:'POST',body:Object.fromEntries(new FormData(form))});sessionStorage.removeItem(draftKey);location.hash=`bug/${result.id}`;}
    catch(error){if(error.status===401){user=null;login('Сеанс завершён. Войдите снова, чтобы продолжить. Черновик сохранён.');}else{formErrors(error,form);document.querySelector('main').dataset.screen='S07E';button.disabled=false;button.textContent='Создать';}}
  });
}

function detail(label,value){return `<div><dt>${label}</dt><dd>${esc(value||'Не указан')}</dd></div>`;}

async function bugCard(bugId,version) {
  const bug=await api('/bugs/'+bugId);const project=await api('/projects/'+bug.projectId);if(version!==renderVersion)return;
  selectedProject=project;document.title=`${bug.code} | BugTrack`;
  shell(`<h1>${esc(bug.code)} ${esc(bug.title)}</h1><p class="status-line"><span>Статус: ${labels.status[bug.status]}</span><span>Приоритет: ${labels.priority[bug.priority]}</span></p>
    <div class="card-grid"><dl class="details">${detail('Описание',bug.description)}${detail('Шаги воспроизведения',bug.steps)}${detail('Ожидаемый результат',bug.expected)}${detail('Фактический результат',bug.actual)}${detail('Окружение',bug.environment)}</dl>
    <dl class="details">${detail('Автор',bug.author)}${detail('Серьёзность',labels.severity[bug.severity])}${detail('Исполнитель','Не назначен')}${detail('Проверяющий',bug.reviewer||'Не назначен')}${detail('Целевой релиз','Не выбран')}${detail('Дедлайн','Не задан')}${detail('Создан (Москва)',date(bug.createdAt))}</dl></div>
    <div class="actions"><a class="btn secondary" href="#bug/${bug.id}/history">История</a><a class="btn secondary" href="#project/${bug.projectId}">К списку</a></div>`,'S08','bugs');
}

async function history(bugId,version) {
  const [bug,entries]=await Promise.all([api('/bugs/'+bugId),api('/bugs/'+bugId+'/history')]);
  const project=await api('/projects/'+bug.projectId);if(version!==renderVersion)return;selectedProject=project;
  document.title=`История ${bug.code} | BugTrack`;
  shell(`<h1>История ${esc(bug.code)}</h1><ul class="history">${entries.map(h=>`<li><time datetime="${esc(h.at)}">${date(h.at)} (Москва)</time><p>${esc(h.actor)} ${h.action==='CREATED'?'создал баг со статусом «Новый»':esc(h.action)}.</p></li>`).join('')}</ul><a class="btn secondary" href="#bug/${bugId}">К карточке</a>`,'history','bugs');
}

function handleError(error){
  if(error.status===401){user=null;selectedProject=null;login('Сеанс завершён. Войдите снова.');}
  else showPageError(error.message);
}

async function render(){
  const version=++renderVersion;
  if(!user){login();return;}
  const parts=location.hash.slice(1).split('/');
  shell('<p role="status">Загрузка…</p>');
  try{
    if(parts[0]==='project'&&parts[1]){if(parts[2]==='new')await newBug(parts[1],version);else await bugList(parts[1],version);}
    else if(parts[0]==='bug'&&parts[1]){if(parts[2]==='history')await history(parts[1],version);else await bugCard(parts[1],version);}
    else await projects(version);
  }catch(error){if(version!==renderVersion)return;handleError(error);if(user){document.querySelector('main').insertAdjacentHTML('beforeend','<a class="btn secondary" href="#projects">Мои проекты</a>');}}
}

window.addEventListener('hashchange',()=>render());
try{user=await api('/auth/me');}catch(error){if(error.status!==401){shell('<h1>BugTrack</h1><p class="message" role="alert">Не удалось загрузить приложение. Обновите страницу после восстановления связи с сервером.</p>');throw error;}}
await render();
