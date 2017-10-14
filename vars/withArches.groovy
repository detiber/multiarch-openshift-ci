def call(List<String> arches=[], Closure body) {
  arch_tasks=[:]
  for (arch in arches) {
    arch_tasks[arch] = {
      task arch
      body()
    }
  }
}
