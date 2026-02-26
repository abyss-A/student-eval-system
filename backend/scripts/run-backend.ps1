$env:JAVA_HOME = "D:\LenovoSoftstore\java-17"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
$mvn = "D:\LenovoSoftstore\Install\IDEA\IntelliJ IDEA 2022.3.2\plugins\maven\lib\maven3\bin\mvn.cmd"
& $mvn spring-boot:run
