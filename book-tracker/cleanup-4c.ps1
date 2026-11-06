# Run from the book-tracker root:  .\cleanup-4c.ps1
$m = "book-service\src\main\java\com\booktracker\booktracker"
$t = "book-service\src\test\java\com\booktracker\booktracker"
$r = "book-service\src\main\resources\db\changelog"

# --- main ---
Remove-Item -Recurse -Force "$m\job"
Remove-Item -Force "$m\config\QuartzConfig.java"
Remove-Item -Force "$m\service\ReadingReportService.java"
Remove-Item -Force "$m\service\ReadingReminderService.java"
Remove-Item -Force "$m\entity\ReadingReport.java"
Remove-Item -Force "$m\repository\ReadingReportRepository.java"
Remove-Item -Force "$r\changes\003-quartz-schema.yaml"
Remove-Item -Force "$r\changes\004-reminders-and-reports.yaml"
Remove-Item -Force "$r\changes\sql\quartz_tables_h2.sql"

# --- tests ---
Remove-Item -Recurse -Force "$t\job"
Remove-Item -Force "$t\config\QuartzConfigTest.java"
Remove-Item -Force "$t\config\QuartzSchemaIT.java"
Remove-Item -Force "$t\service\ReadingReminderServiceIT.java"
Remove-Item -Force "$t\service\ReadingReportServiceIT.java"

Write-Host "Done. Now remove the 'quartz:' block from book-service\src\main\resources\application.yml"