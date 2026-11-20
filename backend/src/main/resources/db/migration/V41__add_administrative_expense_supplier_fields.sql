ALTER TABLE administrative_expense
    ADD COLUMN name VARCHAR(255),
    ADD COLUMN document_number VARCHAR(50);

UPDATE administrative_expense
SET name = CASE
        WHEN description LIKE 'Super sorpresa Panadería - Pastelería - %'
            THEN 'Super sorpresa Panadería - Pastelería'
        WHEN description LIKE '% - %'
            THEN split_part(description, ' - ', 1)
        ELSE NULL
    END,
    document_number = CASE
        WHEN description LIKE 'Ofimatic@.NET%' THEN '25275622-2'
        WHEN description LIKE 'Servientrega S.A%' THEN '860512330-3'
        WHEN description LIKE 'Mónica Sirley Dorado%' THEN '52875796'
        WHEN description LIKE 'Jeronimo Martins Colombia S.A.S.%' THEN '900480569-1'
        WHEN description LIKE 'Super sorpresa Panadería - Pastelería%' THEN '25275622-2'
        WHEN description LIKE 'Casa Yale%' THEN '10536131-4'
        WHEN description LIKE 'Eduar Quila%' THEN '76311826'
        ELSE NULL
    END,
    description = CASE
        WHEN description LIKE 'Super sorpresa Panadería - Pastelería - %'
            THEN substr(description, length('Super sorpresa Panadería - Pastelería - ') + 1)
        WHEN description LIKE '% - %'
            THEN substr(description, strpos(description, ' - ') + 3)
        ELSE description
    END;
