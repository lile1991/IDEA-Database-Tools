## IntellJ IDEA代码自动生成教程
Generate Template POJOs.groovy放到Generate POJOs.groovy相同目录即可使用。

### 特点：
1. 使用灵活: 可以生成所有目录的代码(选择生成到根目录)， 也可以只生成某个子目录的代码(选择生成到子目录)
2. 使用简单： 代码与模板放在一起， 可以随时调整模板， 项目种有非常全的.vm文件样例， 能满足大部分需求。
3. 可以完全替代MyBatis-Generator插件， 可用于生成任意代码，如Entity、DAO、Service、Controller、Mapper XML等

### 使用方法
1. 在IDEA中配置好Database数据源
2. 配置Velocity模板(命名格式：Generate_POJO_{名称}.vm文件)
3. 选择需要生成的表， 即可根据自定义模板生成相关表的


参考链接:
https://segmentfault.com/a/1190000042068048