[![Apache License 2](https://img.shields.io/hexpm/l/plug.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)

# Требования
- OpenJDK 17

# Cтек технологий
- Java 17
- JAX-RS
- Spring Boot 2.7
- Spring Cloud 2021.0.9 (Jubilee)
- N2O Platform 5.3
- Jasperreports Library 6.21.4


[JIRA] | [CI]

# N2O Cloud Report

Микросервис формирования отчетов на основе JasperReports.<br/>
Использует n2o-platform, тем самым включая endpoint актуатора - инструмента для мониторинга доступности сервиса<br/>
Поддерживаемые форматы отчетов: **pdf, xml, csv, xls, xlsx, docx, odt, ods.**<br/>
Есть возможность подключения S3 хранилища для хранения скомпилированных шаблонов отчетов.

# Инструкции

## Настройка

### Настройка библиотеки JasperReports
http://jasperreports.sourceforge.net/config.reference.html

Свойства конфигурации могут быть прописаны как в *jasperreports.properties*,<br/> так и указаны в конкретном шаблоне:
```
<property name="net.sf.jasperreports.export.pdf.encrypted" value="true"/>
```
### Настройка отчета
*parameters.properties* - зависимые от стенда (и другие) параметры отчета.<br/>

При совпадении имен значения, передаваемые в queryParameters, более приоритетны, чем parameters.properties.
Значение *fileStorage.root* внутри шаблона отчета можно использовать, добавив проперти *fsRoot*
### Настройка хранилища шаблонов отчетов
По умолчанию для хранения скомпилированных шаблонов используется каталог локальной файловой системы сервиса. 
Каталог определяется переменной *fileStorage.root*.

Для подключения S3 хранилища необходимо указать следующие переменные:
```yaml
report-service:
  s3:
    access-key: "<login>"
    secret-key: "<password>"
    endpoint: "https://minio-develop.i-novus.ru"
    bucket-name: "my-reports"        
```

## Использование
1. Создать шаблон отчета (.jrxml) с помощью Jaspersoft Studio или других средств.
2. Результат скомпилировать и сохранить в файловое хранилище, выполнив POST запрос (на вход .jrxml)
3. Сгенерировать отчет, выполнив GET запрос

## Примеры

**Использование Data Adapter в шаблонах не требуется и приведет к ошибкам (net.sf.jasperreports.data.adapter).**

1. Скомпилировать шаблоны (POST):<br/> 
[accrCommand.jrxml](accrCommand.jrxml)<br/>
[command.jrxml](command.jrxml) - сабрепорт, используемый в accrCommand.jrxml <br/><br/>
Сформировать отчет (GET):<br/> 
http://localhost:8080/api/report/accrCommand.pdf?status=ACCREDITED<br/><br/>
Датасорс для главного репорта и сабрепорта заданы через параметр **net.sf.jasperreports.json.source** (http://jasperreports.sourceforge.net/config.reference.html).
Датасорс сабрепорта должен задаваться в главном.<br/><br/> 

2. Скомпилировать шаблон (POST):<br/>
[periodReport.jrxml](periodReport.jrxml) <br/><br/>
Сформировать отчет (GET):<br/>
http://localhost:8080/api/report/periodReport.pdf?baseUrl=http://docker.one:8396/safekids/api&fromDt=2020-03-01&toDt=2020-03-15 <br/> 
baseUrl можно прописать в parameters.properties,<br/>
fromDt и toDt в данном примере должны быть в рамках одного месяца<br/><br/>
В репорте определен subDataset, который используется для заполнения таблицы. Таких subDataset'ов может быть несколько, но при этом **датасорс уже не должен быть задан** (как в первом примере). 
Обязательные параметры датасета: uriKey, authTypeKey, authParametersKey, languageKey, verbKey<br/><br/> 

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
2. В свойствах самого .jrxml - файла прописать REPORT_DATASOURCE_NAME, например 
```xml
<property name="REPORT_DATASOURCE_NAME" value="lkzeo"/>
```
# Примечания

## Вложенные изображения в base64
Для декодирования вложенных изображений в формате base64 следует использовать org.apache.commons.codec.binary.Base64.decodeBase64. Например
```xml
<imageExpression class="java.io.InputStream"><![CDATA[new java.io.ByteArrayInputStream(org.apache.commons.codec.binary.Base64.decodeBase64($P{logo_base64}.getBytes()))]]></imageExpression>
```

## Параметры-коллекции
Иногда в отчет требуется передать параметр, содержащий несколько значений.
Пример: из БД требуется получить все записи с идентификатором, принимающим значения из списка (значение_id_1, значение_id_2,...). В таком случае можно выполнить SQL-запрос вида: 
```sql
select * from "table_name" where id in (:parameter)
```
Для того, чтобы передать параметр, содержащий коллекцию значений, необходимо в отчете создать параметр:
```xml
<parameter name="id_list" class="java.util.Collection" nestedType="java.lang.Integer" isForPrompting="false"/>
```
Для добавления параметра-коллекции в запрос следует использовать функцию: `$X{IN,поле_таблицы, параметр-коллекция}`, которая при встраивании в SQL запрос формирует предикат: поле_таблицы IN (:параметр_коллекции)
Таким образом запрос в отчете примет вид:
```xml
<queryString language="SQL">
    <![CDATA[SELECT id, name, FROM some_table WHERE $X{IN,id, id_list}]]>
</queryString>
```
Как правило требуется проверить передаваемый в отчет параметр-коллекцию на `null` или `isEmpty()`. Это можно сделать, создав отдельный параметр запроса, содержащий проверку:
```xml
<parameter name="id_predicate" class="java.lang.String" isForPrompting="false">
    <defaultValueExpression><![CDATA[($P{id_list} == null || $P{id_list}.isEmpty())?"true": "$X{IN,id, id_list}"]]></defaultValueExpression>
</parameter>
```
В default-выражении параметра `id_predicate` выполняется проверка на `null` и на пустую коллекцию параметра `id_list`. Если `id_list` равен `null` или пустой коллекции, то предикат будет равен `true`.
В этом случае запрос будет выглядеть следующим образом:
```xml
<queryString language="SQL">
    <![CDATA[SELECT id, name, FROM some_table WHERE $P!{id_predicate}]]>
</queryString>
```
Обратите внимание на восклицательный знак в `$P!{id_predicate}`. Он говорит о том, что в запрос передается вычисленное значение параметра (в нашем случае это либо "true", либо "$X{IN,id, id_list}").
Если не указать восклицательный знак, то компилятор Jasper попытается передать `id_predicate` как SQL-парамeтр, и конечный запрос, который передается в БД примет вид: 
```SELECT id, name, FROM some_table WHERE :id_predicate```  ->  ```SELECT id, name, FROM some_table WHERE ($P{id_list} == null || $P{id_list}.isEmpty())?"true": "$X{IN,id, id_list}"```. 
В результате возникнет ошибка. 

## Поддержка UUID, LocalDate, LocalDateTime
В 6 версии jasper-report нет поддержки типов UUID, LocalDate, LocalDateTime для параметров отчета. 
Приходится работать с ними как со строками.

В 7 версии появилась поддержка LocalDate и LocalDateTime:

https://github.com/TIBCOSoftware/jasperreports/blob/master/core/src/main/java/net/sf/jasperreports/engine/query/JRJdbcQueryExecuterFactory.java

## Экспорт отчета в формате xls
### Свойства, улучшающие отображение отчета при экспорте в формате xls
Чтобы таблица не выглядела разделенной на страницы при экспорте в формате xls, можно воспользоваться custom-property:
```xml
<property name="net.sf.jasperreports.export.xls.remove.empty.space.between.rows" value="true"/>
```
– удаляет пустые строки

В ячейках Excel нет отступов (paddings). Для того, чтобы текст не "налезал" на края ячеек, можно воспользоваться свойством:
```xml
<property name="net.sf.jasperreports.export.xls.font.size.fix.enabled" value="true"/>
```
– фактически уменьшает шрифт на 1pt.

Чтобы в xls не закрашивался background ячеек, можно воспользоваться свойством:
```xml
<property name="net.sf.jasperreports.export.xls.white.page.background" value="false"/>
```

### Скрытие элементов при экспорте
Чтобы скрыть элемент в отчете при экспорте в определенном формате есть несколько способов:

#### Можно воспользоваться свойствами исключения элементов
```xml
net.sf.jasperreports.export.{format}.exclude.origin.{suffix}.{arbitrary_name} net.sf.jasperreports.export.{format}.exclude.origin.keep.first.{suffix}.{arbitrary_name} 
```
Подробнее описано тут:<br/>
https://community.jaspersoft.com/knowledgebase/faq/how-can-i-suppress-page-headers-and-footers-when-exporting-xls/ 

У семейства этих свойств при совпадении {arbitrary_name} свойства действуют в совокупности (уточняя условия друг друга).
Например:
```xml
<property name="net.sf.jasperreports.export.xls.exclude.origin.keep.first.band.1" value="columnHeader"/> 
<property name="net.sf.jasperreports.export.xls.exclude.origin.keep.first.report.1" value="*"/> 
```
– два свойства исключают повторение элемента (keep.first) columnHeader у всех отчетов (и таблиц) при экспорте отчета в формате xls.

Исключать элементы можно также по ключу (key) (не у всех элементов есть такое поле):
```xml
<property name="net.sf.jasperreports.export.xls.exclude.key.{keyvalue}" /> 
```

#### TableHeader вместо ColumnHeader
У элемента *Table* есть две шапки: *TableHeader* и *ColumnHeader*.<br/>
*TableHeader* отображается только один раз - на первой странице. *ColumnHeader* повторяется на каждой таблице.<br/>
Если приемлемо, чтобы таблица отображалась без повтора шапки в разных форматах экспорта (pdf, xls, odt), то самый простой способ - воспользоваться *TableHeader* вместо *ColumnHeader*.


### Разделение на отдельные листы при экпорте в xls
Для того, чтобы начать новый Excel-лист, служит custom-property:
```xml
<property name="net.sf.jasperreports.export.xls.break.before.row" value="true"/> 
```
– данное свойство помечает элемент отчета, с которого будет начинаться новый лист в xls-документе. 
Можно пометить Static Text, Text Field... 
Не работает с Table. Чтобы указать таблицу, необходимо "повесить" данное свойство например на Static Text заголовка первого столбца. 

В паре с данным свойством можно указать еще одно:
```xml
<property name="net.sf.jasperreports.export.xls.sheet.name" value="Наименование листа"/> 
```
– служит для указания наименования листа.

Могут также возникнуть более сложные условия разделения на листы (например разделение на листы по подотчетам, разделение на листы по группам и другие...)<br/>
В community jaspersoft есть серия примеров: <br/>
[Creating Multi-sheet xls Report Output with a Tabular Report](https://community.jaspersoft.com/knowledgebase/code-sample/sample-report-creating-multi-sheet-xls-report-output-tabular-report/) <br/>
[Creating Multi-sheet xls Report Output with a Table Report](https://community.jaspersoft.com/knowledgebase/code-sample/sample-report-creating-multi-sheet-xls-report-output-table-report/) <br/>
[Creating Multi-sheet xls Report Output with Group Headers as Column Headers](https://community.jaspersoft.com/knowledgebase/code-sample/sample-report-creating-multi-sheet-xls-report-output-group-headers-column-headers/) <br/>
[Creating Multi-sheet xls Report Output with Subreports](https://community.jaspersoft.com/knowledgebase/code-sample/sample-report-creating-multi-sheet-xls-report-output-subreports/) <br/>
[Creating Multi-sheet xls Report Output with Dual Purpose](https://community.jaspersoft.com/knowledgebase/code-sample/sample-report-creating-multi-sheet-xls-report-output-dual-purpose/) <br/>