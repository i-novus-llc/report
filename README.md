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



[JIRA]: https://jira.i-novus.ru/projects/REPENG
[CI]: https://ci.i-novus.ru/view/n2o-components/job/report