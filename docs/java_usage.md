## Windows

-------------------

### Check if you have the JDK (compiler)
```javac -version```

### Check if JAVA_HOME is set (in PowerShell)
```$env:JAVA_HOME```

### Or in Command Prompt
```echo %JAVA_HOME%```

### Check build tool (Maven or Gradle)
```mvn -version```
or
```gradle -version```

### Run build tool (Maven or Gradle)

```.\mvnw.cmd spring-boot:run```
or
```.\gradlew.bat bootRun```

```.\gradlew clean bootRun```

### Update dependencies

```.\gradlew build --refresh-dependencies```

```.\gradlew :synopsi-api:build --refresh-dependencies```

### Run tests

```.\gradlew test```

-------------------

## Trouble Shoot

### Find Java
```where java```


```$env:JAVA_HOME```

```Get-Command java | Select-Object -ExpandProperty Source```

This will show you the path to your Java installation. It's likely something like:

```C:\Program Files\Java\jdk-22.0.2```

### Set JAVA_HOME
```[System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Java\jdk-22', 'Machine')```
```$env:JAVA_HOME = "C:\Program Files\Java\jdk-22"```


Here is the equivalent of your markdown document for a Bash environment.

## Bash

-------------------

### Check if you have the JDK (compiler)
```javac -version```

### Check if JAVA_HOME is set
```echo $JAVA_HOME```

### Check build tool (Maven or Gradle)
```mvn -version```
or
```gradle -version```

### Run build tool (Maven or Gradle)

```./mvnw spring-boot:run```
or
```./gradlew bootRun```

-------------------

## Trouble Shoot

### Find Java
To find the location of the `java` executable, you can use the `which` or `whereis` command.

```which java```

```whereis java```

This will show you the path to your Java installation. It's likely something like:

`/usr/lib/jvm/java-11-openjdk-amd64/bin/java`

### Set JAVA_HOME
To set the `JAVA_HOME` environment variable for the current session, use the `export` command. To make this change 
permanent, you can add the command to your shell's configuration file, such as `~/.bashrc` or `~/.bash_profile`.

```export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64```

After adding it to your configuration file, you can apply the changes to your current session by running:

```source ~/.bashrc``` or ```source ~/.bash_profile```