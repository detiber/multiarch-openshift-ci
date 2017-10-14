def call(List<String> arches=[], Closure body) {
  arch_tasks=[:]
  for (arch in arches) {
    arch_tasks[arch] = {
      def task_name = arch
      task task_name
      body()
    }
  }
  parallel arch_tasks
}
