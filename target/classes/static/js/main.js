document.addEventListener('DOMContentLoaded', function() {
    // Görevleri yükle
    loadTasks();

    // Event listeners
    document.getElementById('createTaskBtn').addEventListener('click', showCreateTaskModal);
    document.getElementById('myTasksBtn').addEventListener('click', loadMyTasks);
    document.getElementById('assignedTasksBtn').addEventListener('click', loadAssignedTasks);
    document.getElementById('saveTaskBtn').addEventListener('click', saveTask);
});

function loadTasks() {
    fetch('/api/tasks')
        .then(response => response.json())
        .then(tasks => {
            const taskList = document.getElementById('taskList');
            taskList.innerHTML = '';
            tasks.forEach(task => {
                taskList.appendChild(createTaskCard(task));
            });
        });
}

function createTaskCard(task) {
    const card = document.createElement('div');
    card.className = 'col-md-4';
    card.innerHTML = `
        <div class="card task-card priority-${task.priority.toLowerCase()}">
            <div class="card-header">
                <h5 class="card-title">${task.title}</h5>
                <span class="task-status status-${task.status.toLowerCase()}">${task.status}</span>
            </div>
            <div class="card-body">
                <p>${task.description}</p>
                <p><strong>Bitiş:</strong> ${formatDate(task.deadline)}</p>
                <div class="d-flex justify-content-between">
                    <button class="btn btn-sm btn-primary" onclick="showTaskDetails(${task.id})">Detaylar</button>
                    ${task.status !== 'COMPLETED' ? 
                        `<button class="btn btn-sm btn-success" onclick="completeTask(${task.id})">Tamamla</button>` : ''}
                </div>
            </div>
        </div>
    `;
    return card;
}

function showCreateTaskModal() {
    const modal = new bootstrap.Modal(document.getElementById('createTaskModal'));
    // Kullanıcıları yükle
    loadUsers().then(() => {
        modal.show();
    });
}

function saveTask() {
    const form = document.getElementById('createTaskForm');
    const formData = new FormData(form);
    
    fetch('/api/tasks', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(Object.fromEntries(formData))
    })
    .then(response => response.json())
    .then(task => {
        bootstrap.Modal.getInstance(document.getElementById('createTaskModal')).hide();
        loadTasks();
    });
}

function formatDate(dateString) {
    return new Date(dateString).toLocaleString('tr-TR');
}

function loadUsers() {
    return fetch('/api/users')
        .then(response => response.json())
        .then(users => {
            const select = document.querySelector('select[name="assignees"]');
            select.innerHTML = users.map(user => 
                `<option value="${user.id}">${user.firstName} ${user.lastName}</option>`
            ).join('');
        });
} 