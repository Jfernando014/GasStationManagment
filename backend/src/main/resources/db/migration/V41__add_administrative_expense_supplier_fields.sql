ALTER TABLE administrative_expense
    ADD COLUMN name VARCHAR(255),
    ADD COLUMN document_number VARCHAR(50);

UPDATE administrative_expense
SET name = split_part(description, ' - ', 1),
    document_number = CASE
        WHEN description LIKE 'Ofimatic@.NET%' THEN '25275622-2'
        WHEN description LIKE 'Servientrega S.A%' THEN '880512330-3'
        WHEN description LIKE 'Mónica Sirley Dorado%' THEN '52375796'
        WHEN description LIKE 'Jeronimo Martins Colombia S.A.S.%' THEN '900480589-1'
        WHEN description LIKE 'Casa Yale%' THEN '10536131-4'
        ELSE NULL
    END,
    description = CASE
        WHEN description LIKE '% - %' THEN substr(description, strpos(description, ' - ') + 3)
        ELSE description
    END;
