[JIRA] | [CI]

# N2O Cloud Report

Микросервис формирования отчетов на основе JasperReports.<br/>
Поддерживаемые форматы отчетов: **pdf, xml, csv, xls, xlsx, docx, odt, ods.**

# Инструкции

## Настройка

1. **Настройка библиотеки JasperReports.**<br/>
    http://jasperreports.sourceforge.net/config.reference.html
    
    Свойства конфигурации могут быть прописаны как в **jasperreports.properties**,<br/> так и указаны в конкретном шаблоне: 

```xml
    <property name="net.sf.jasperreports.export.pdf.encrypted" value="true"/>
```
     
## Использование
Пример запроса:
http://localhost:8080/api/report/form345.pdf?param1=p1?param2=p2

Значение **fileStorage.root** внутри шаблона отчета можно использовать, добавив проперти **fsRoot**

[JIRA]: https://jira.i-novus.ru/projects/REPENG
[CI]: https://ci.i-novus.ru/view/n2o-components/job/report