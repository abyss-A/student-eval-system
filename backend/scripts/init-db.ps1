param(
  [string]$HostName = "localhost",
  [int]$Port = 3306,
  [string]$UserName = "root",
  [string]$Password = "123456",
  [switch]$RecreateSchema
)

$mysql = "D:\LenovoSoftstore\Install\MySQL\MySQL Server 8.0\bin\mysql.exe"
if (!(Test-Path $mysql)) {
  Write-Error "mysql client not found: $mysql"
  exit 1
}

$seedSql = Resolve-Path "./src/main/resources/db/seed.sql"

if ($RecreateSchema) {
  $initSql = Resolve-Path "./src/main/resources/db/init.sql"
  $cmd1 = "`"$mysql`" -h $HostName -P $Port -u $UserName -p$Password < `"$initSql`""
  cmd /c $cmd1
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

$cmd2 = "`"$mysql`" -h $HostName -P $Port -u $UserName -p$Password < `"$seedSql`""
cmd /c $cmd2
exit $LASTEXITCODE
