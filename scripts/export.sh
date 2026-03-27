#!/bin/bash
output="project_export_$(date +%Y%m%d_%H%M%S).txt"

# Укажите нужные расширения
extensions="java kt xml yaml yml properties gradle kts"

for ext in $extensions; do
    find . -type f -name "*.$ext" \
        -not -path "*/target/*" \
        -not -path "*/build/*" \
        -not -path "*/.gradle/*" \
        -not -path "*/.idea/*" \
        -not -path "*/node_modules/*" \
        | sort \
        | while read file; do
            echo "=== $file ===" >> "$output"
            cat "$file" >> "$output"
            echo "" >> "$output"
            echo "" >> "$output"
        done
done

echo "Экспорт завершен. Файл: $output"
echo "Всего файлов: $(grep -c "^=== " "$output")"