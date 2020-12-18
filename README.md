[![Apache License 2](https://img.shields.io/hexpm/l/plug.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)

# Требования
- OpenJDK 14

# Cтек технологий
- Java 14
- JAX-RS
- Spring Boot 2.1
- Spring Cloud Greenwich
- N2O Platform 4


[JIRA] | [CI]

# N2O Cloud Report

Микросервис формирования отчетов на основе JasperReports.<br/>
Использует n2o-platform, тем самым включая endpoint актуатора - инструмента для мониторинга доступности сервиса 
Поддерживаемые форматы отчетов: **pdf, xml, csv, xls, xlsx, docx, odt, ods.**

# Инструкции

## Настройка

1. **Настройка библиотеки JasperReports.**<br/>
    http://jasperreports.sourceforge.net/config.reference.html
    
    Свойства конфигурации могут быть прописаны как в *jasperreports.properties*,<br/> так и указаны в конкретном шаблоне: 

    ```
    <property name="net.sf.jasperreports.export.pdf.encrypted" value="true"/>
    ```
 
2. **Настройка отчета.**<br/>
*parameters.properties* - зависимые от стенда (и другие) параметры отчета.<br/> 

    При совпадении имен значения, передаваемые в queryParameters, более приоритетны, чем parameters.properties.

    Значение *fileStorage.root* внутри шаблона отчета можно использовать, добавив проперти *fsRoot*

       
## Использование
1. Создать шаблон отчета (.jrxml) с помощью Jaspersoft Studio или других средств.
2. Результат скомпилировать и сохранить в файловое хранилище, выполнив POST запрос (на вход .jrxml)
3. Сгенерировать отчет, выполнив GET запрос

## Примеры

1. Скомпилировать шаблоны (POST):<br/> 
[accrCommand.jrxml](accrCommand.jrxml)<br/>
[command.jrxml](command.jrxml) - сабрепорт, используемый в accrCommand.jrxml <br/><br/>
Сформировать отчет (GET):<br/> 
http://localhost:8080/api/report/accrCommand.pdf?status=ACCREDITED<br/><br/>

2. Скомпилировать шаблон (POST):<br/>
[periodReport.jrxml](periodReport.jrxml) <br/><br/>
Сформировать отчет (GET):<br/>
http://localhost:8080/api/report/periodReport.pdf?baseUrl=http://docker.one:8396/safekids/api&fromDt=2020-03-01&toDt=2020-03-15 <br/> 
baseUrl можно прописать в parameters.properties,<br/>
fromDt и toDt в данном примере должны быть в рамках одного месяца

## Подключение к базе данных.
Имеется возможность настраивать несколько подключений к БД. Нужно указать
``` 
 report.data.values[{index}].name - название, служит для привязки отчета
 report.data.values[{index}].driver - драйвер DataSource
 report.data.values[{index}].username - пользователь бд
 report.data.values[{index}].password - пароль бд
 report.data.values[{index}].url - url бд
 где {index} - порядковый номер подключения к бд
 ```
 Настройка происходит стандатным способом для Spring Boot приложений через properties/yml файлы. Например можно прописать в application.properties
 ```properties
report.data.values[0].name=lkzeo
report.data.values[0].driver=org.postgresql.Driver
report.data.values[0].username=lk_user
report.data.values[0].password=lk_psw
report.data.values[0].url=jdbc:postgresql://localhost:5432/lkzeo

report.data.values[1].name=idm
report.data.values[1].driver=org.postgresql.Driver
report.data.values[1].username=idm_user
report.data.values[1].password=idm_psw
report.data.values[1].url=jdbc:postgresql://localhost:5432/idm
```
Существуют два способа связать отчет с конкретным датасорсом 
1. В запросе вызова отчета указать параметр REPORT_DATASOURCE_NAME, например  http://localhost:8080/api/report/report.pdf?REPORT_DATASOURCE_NAME=lkzeo
2. В параметрах самого .jrxml - файла прописать REPORT_DATASOURCE_NAME, например 
```xml
<parameter name="REPORT_DATASOURCE_NAME" class="java.lang.String">
    <defaultValueExpression><![CDATA[lkzeo]]></defaultValueExpression>
</parameter>
```

## Вложенные изображения в base64
Для декодирования вложенных изображений в формате base64 следует использовать org.apache.commons.codec.binary.Base64.decodeBase64. Например
```xml
<imageExpression class="java.io.InputStream"><![CDATA[new java.io.ByteArrayInputStream(org.apache.commons.codec.binary.Base64.decodeBase64($P{logo_base64}.getBytes()))]]></imageExpression>
```