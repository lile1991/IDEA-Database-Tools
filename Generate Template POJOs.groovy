import com.intellij.database.model.DasTable
import com.intellij.database.model.ObjectKind
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

import java.io.StringWriter
import java.util.HashSet

import org.apache.velocity.VelocityContext
import org.apache.velocity.Template
import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.app.Velocity
import org.apache.velocity.exception.ResourceNotFoundException
import org.apache.velocity.exception.ParseErrorException
import org.apache.velocity.exception.MethodInvocationException
import org.apache.velocity.runtime.RuntimeConstants

/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */

typeMapping = [
        (~/(?i)bit/)                   : "Boolean",
        (~/(?i)bigint/)                   : "Long",
        (~/(?i)tinyint/)                  : "Byte",
        (~/(?i)int/)                      : "Integer",
        (~/(?i)float|double|decimal|real/): "java.math.BigDecimal",
        (~/(?i)datetime|timestamp/)       : "java.util.Date",
        (~/(?i)date/)                     : "java.util.Date",
        (~/(?i)time/)                     : "java.sql.Time",
        (~/(?i)/)                         : "String"
]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
    SELECTION.filter { it instanceof DasTable && it.getKind() == ObjectKind.TABLE }.each { generate(it, dir) }
}


VelocityEngine velocityEngine() {
    // 初始化模版引擎
    VelocityEngine ve = new VelocityEngine()
    // 这两个属性可以从RuntimeConstants常量中找到， 引用常量有些版本报错， 就直接写死了
    ve.setProperty("runtime.log", PROJECT.getBaseDir().path + "/dbTools.log") // 对应RuntimeConstants.RUNTIME_LOG
    ve.setProperty("file.resource.loader.path", PROJECT.getBaseDir().path)
    // 对应RuntimeConstants.FILE_RESOURCE_LOADER_PATH
    ve.init()
    ve
}

List<File> listVmFiles(File dir) {
    return listVmFiles0(dir, new ArrayList<>());
}

List<File> listVmFiles0(File dir, List<File> fileList) {
    File[] files = dir.listFiles();
    if(files == null) {
        return fileList;
    }

    for(File file: files) {
        if(file.isDirectory()) {
            listVmFiles0(file, fileList);
        } else {
            String fileName = file.getName();
            if(fileName.startsWith("Generate_POJO_") && fileName.endsWith(".vm")) {
                fileList.add(file);
            }
        }
    }
    return fileList;
}

def generate(table, dir) {
    // 转驼峰， 如t_user转换为TUser
    def className = javaName(table.getName(), true)

    // 读取所有模板文件
    List<File> templateFileList = listVmFiles(dir)
    if(templateFileList == null || templateFileList.isEmpty()) {
        return
    }

    String templateEncoding = "UTF-8"
    VelocityEngine ve = velocityEngine()

    // 根据模板循环生成代码
    for(File templateFile: templateFileList) {
        def templatePath = templateFile.getAbsolutePath().substring(PROJECT.getBaseDir().path.length() + 1)
        Template template = ve.getTemplate(templatePath, templateEncoding)

        VelocityContext ctx = new VelocityContext()
        // 设置变量
        setContextProperty(ctx, table, className, dir)

        // 输出
        StringWriter sw = new StringWriter()
        template.merge(ctx, sw)
        String generateContent = sw.toString()

        // 默认生成的文件名
        String fileName = className + ".java";

        // 读取属性定义
        def lines = generateContent.split("\n")

        // 存储正文(去除属性定义)
        sw = new StringWriter()

        boolean isDefinedReading = true
        for(int i = 0; i < lines.length; i ++){
            def line = lines[i]
            if(isDefinedReading) {
                // 先读取属性定义
                if(line.startsWith("#fileName=")) {
                    fileName = line.substring("#filename=".length()).trim()
                    continue
                }

                isDefinedReading = false
            }
            // 再读取正文
            sw.append(line).append("\r\n")
        }

//        new File(new File("D:\\"), fileName).append(fileName)

        new File(templateFile.getParentFile(),  fileName).withPrintWriter("UTF-8") { out -> out.print sw }
    }
}

def setContextProperty(ctx, table, className, dir) {
    // 首字母小写
    def memberName = String.valueOf(Character.toLowerCase(className.charAt(0))) + className.substring(1)

    // 将类信息放入模板变量
    ctx.put("class", [
            "name"   : className,   // 类名， 首字母大写
            "member"   : memberName,   // 类名, 首字母小写
            "comment": table.comment    // 表注释
    ])

    ctx.put("table", [
            "name": table.getName()
    ])

    def cmbFields = calcFields(table)
    // 将字段信息放入模板变量
    ctx.put("imports", cmbFields.imports)
    ctx.put("fields", cmbFields.fields)
}

def calcFields(table) {
    def imports = [] as HashSet
    def fields = DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDataType().getSpecification())
        def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value

        if (typeStr.contains(".")) {
            imports << typeStr
            typeStr = typeStr.substring(typeStr.lastIndexOf(".") + 1)
        }

        def jdbcType = Case.UPPER.apply(col.getDataType().typeName)

        if(jdbcType == "TINYINT" && col.getDataType().size == 1) {
            // 一个长度的TINYINT, 用Boolean类型
            typeStr = "Boolean"
            jdbcType = "BIT"
        }

        fields += [[
                           name   : javaName(col.getName(), false),
                           type   : typeStr,
                           colName: col.getName(),
//                           jdbcType: Case.LOWER.apply(col.getDataType().getSpecification()),
                           jdbcType: jdbcType == "INT" ? "INTEGER" : jdbcType,
                           sizeUnit: col.getDataType().sizeUnit,
                           size: col.getDataType().size,
                           comment: col.comment,
                           annos  : ""]]

    }
    ["fields": fields, "imports": imports]
}

def javaName(str, capitalize) {
    def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
            .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
    capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}