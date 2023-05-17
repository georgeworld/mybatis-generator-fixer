# mybatis-generator-fixer
## mybatis generator的一个修补器maven插件，用来对mybatis generator生成的文件，进行二次修改。

## 使用方式: 
- 1,将本maven插件下载到本地,在项目根路径命令行执行:mvn install
- 2,在你自己项目的pom.xml文件中添加:
    
    ```
    <plugin>
        <groupId>com.georgeinfo.maven.plugin</groupId>
        <artifactId>mybatis-generator-fixer</artifactId>
        <version>1.0-SNAPSHOT</version>
        <configuration>
            <!-- Maven 内置属性:项目根路径 -->
            <basedir>${basedir}</basedir>
            <scanPackage>要扫描的包路径</scanPackage>
        </configuration>
    </plugin>
    ```
- 3,利用idea运行插件
 选中插件mybatis-generator-fixer,点击运行按钮即可
