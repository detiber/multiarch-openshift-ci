def call(List<String> arches=[], Closure body) {
  arch_tasks=[:]
  for (arch in arches) {
    arch_tasks[arch] = {
      task arch
      body()
    }
  }
  task 'before parallel'
  echo 'before parallel'
  parallel arch_tasks
  task 'after parallel'
  echo 'after parallel'
}
