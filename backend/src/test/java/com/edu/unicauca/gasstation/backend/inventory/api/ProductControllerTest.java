package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import com.edu.unicauca.gasstation.backend.inventory.ProductTestData;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.ProductRequest;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Product;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.ProductService;
import com.edu.unicauca.gasstation.backend.inventory.exception.DuplicateProductCodeException;
import com.edu.unicauca.gasstation.backend.inventory.exception.ProductNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.ProductMapperImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.context.TestPropertySource;
import org.springframework.context.annotation.FilterType;
import com.edu.unicauca.gasstation.backend.security.infrastructure.jwt.JwtAuthenticationFilter;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ProductController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(ProductMapperImpl.class)
@TestPropertySource(properties = "server.port=0")
class ProductControllerTest {

    private static final String BASE_URL = "/api/v1/inventory/products";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    // ---------- POST ----------

    @Test
    void createProductReturnsCreatedWithTheProductAndMargin() throws Exception {
        when(productService.create(any(Product.class)))
                .thenReturn(ProductTestData.product(1L, "80216", "Terpel Celerity 4T Cuarto"));

        postProduct(validRequest())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("80216"))
                .andExpect(jsonPath("$.category").value("LUBRICANTE"))
                .andExpect(jsonPath("$.price").value(28500))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.marginPercent").value(23.07));
    }

    @Test
    void createProductWithoutCostReturnsNullMargin() throws Exception {
        Product withoutCost = ProductTestData.product(1L, "80216", "Producto");
        withoutCost.setCost(null);
        when(productService.create(any(Product.class))).thenReturn(withoutCost);
        ProductRequest request = validRequest();
        request.setCost(null);

        postProduct(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cost", nullValue()))
                .andExpect(jsonPath("$.marginPercent", nullValue()));
    }

    @Test
    void createProductRejectsBlankCode() throws Exception {
        ProductRequest request = validRequest();
        request.setCode("  ");

        postProduct(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("code"))
                .andExpect(jsonPath("$.fields[0].message").value(ProductRequest.CODE_REQUIRED));
        verifyNoInteractions(productService);
    }

    @Test
    void createProductRejectsTooLongCode() throws Exception {
        ProductRequest request = validRequest();
        request.setCode("1".repeat(21));

        postProduct(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("code"))
                .andExpect(jsonPath("$.fields[0].message").value(ProductRequest.CODE_SIZE));
        verifyNoInteractions(productService);
    }

    @Test
    void createProductRejectsBlankName() throws Exception {
        ProductRequest request = validRequest();
        request.setName("");

        postProduct(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("name"))
                .andExpect(jsonPath("$.fields[0].message").value(ProductRequest.NAME_REQUIRED));
        verifyNoInteractions(productService);
    }

    @Test
    void createProductRejectsMissingCategory() throws Exception {
        ProductRequest request = validRequest();
        request.setCategory(null);

        postProduct(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("category"))
                .andExpect(jsonPath("$.fields[0].message").value(ProductRequest.CATEGORY_REQUIRED));
        verifyNoInteractions(productService);
    }

    @Test
    void createProductRejectsZeroPrice() throws Exception {
        ProductRequest request = validRequest();
        request.setPrice(BigDecimal.ZERO);

        postProduct(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("price"))
                .andExpect(jsonPath("$.fields[0].message").value(ProductRequest.PRICE_POSITIVE));
        verifyNoInteractions(productService);
    }

    @Test
    void createProductRejectsMissingPrice() throws Exception {
        ProductRequest request = validRequest();
        request.setPrice(null);

        postProduct(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("price"))
                .andExpect(jsonPath("$.fields[0].message").value(ProductRequest.PRICE_REQUIRED));
        verifyNoInteractions(productService);
    }

    @Test
    void createProductRejectsNegativeCost() throws Exception {
        ProductRequest request = validRequest();
        request.setCost(new BigDecimal("-1"));

        postProduct(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("cost"))
                .andExpect(jsonPath("$.fields[0].message").value(ProductRequest.COST_NOT_NEGATIVE));
        verifyNoInteractions(productService);
    }

    @Test
    void createProductAcceptsZeroCost() throws Exception {
        when(productService.create(any(Product.class)))
                .thenReturn(ProductTestData.product(1L, "80216", "Producto"));
        ProductRequest request = validRequest();
        request.setCost(BigDecimal.ZERO);

        postProduct(request).andExpect(status().isCreated());
    }

    @Test
    void createProductRejectsMissingActive() throws Exception {
        ProductRequest request = validRequest();
        request.setActive(null);

        postProduct(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("active"))
                .andExpect(jsonPath("$.fields[0].message").value(ProductRequest.ACTIVE_REQUIRED));
        verifyNoInteractions(productService);
    }

    @Test
    void createProductRejectsUnknownCategory() throws Exception {
        String json = """
                {"code":"80216","name":"Producto","category":"JUGUETES",
                 "price":28500,"cost":21924,"active":true}
                """;

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Invalid data"));
        verifyNoInteractions(productService);
    }

    @Test
    void createProductReturnsConflictWhenCodeAlreadyExists() throws Exception {
        when(productService.create(any(Product.class))).thenThrow(new DuplicateProductCodeException("80216"));

        postProduct(validRequest())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe un producto con el código 80216"));
    }

    // ---------- GET ----------

    @Test
    void findAllWithoutFiltersReturnsEveryProduct() throws Exception {
        when(productService.findAll(null, null)).thenReturn(List.of(
                ProductTestData.product(1L, "80216", "Producto uno"),
                ProductTestData.product(2L, "80262", "Producto dos")));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("80216"))
                .andExpect(jsonPath("$[1].code").value("80262"));
    }

    @Test
    void findAllPassesCategoryAndActiveFilters() throws Exception {
        when(productService.findAll(ProductCategory.ADITIVO, true)).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL).param("category", "ADITIVO").param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        verify(productService).findAll(ProductCategory.ADITIVO, true);
    }

    @Test
    void findAllRejectsUnknownCategory() throws Exception {
        mockMvc.perform(get(BASE_URL).param("category", "JUGUETES"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid data"))
                .andExpect(jsonPath("$.message").value("The value of parameter 'category' is invalid"));
        verifyNoInteractions(productService);
    }

    @Test
    void findByIdReturnsTheProduct() throws Exception {
        when(productService.findById(1L)).thenReturn(ProductTestData.product(1L, "80216", "Producto"));

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("80216"));
    }

    @Test
    void findByIdReturnsNotFoundWhenProductDoesNotExist() throws Exception {
        when(productService.findById(99L)).thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get(BASE_URL + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se encontró el producto con id 99"))
                .andExpect(jsonPath("$.path").value(BASE_URL + "/99"));
    }

    // ---------- PUT ----------

    @Test
    void updateProductReturnsTheUpdatedProduct() throws Exception {
        when(productService.update(eq(1L), any(Product.class)))
                .thenReturn(ProductTestData.product(1L, "80217", "Nombre nuevo"));
        ProductRequest request = validRequest();
        request.setCode("80217");

        putProduct(1L, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("80217"));
    }

    @Test
    void updateProductReturnsNotFoundWhenProductDoesNotExist() throws Exception {
        when(productService.update(eq(99L), any(Product.class))).thenThrow(new ProductNotFoundException(99L));

        putProduct(99L, validRequest()).andExpect(status().isNotFound());
    }

    @Test
    void updateProductReturnsConflictWhenAnotherProductUsesTheCode() throws Exception {
        when(productService.update(eq(1L), any(Product.class))).thenThrow(new DuplicateProductCodeException("80262"));

        putProduct(1L, validRequest())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe un producto con el código 80262"));
    }

    @Test
    void updateProductRejectsInvalidFields() throws Exception {
        ProductRequest request = validRequest();
        request.setPrice(new BigDecimal("-5"));

        putProduct(1L, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("price"));
        verifyNoInteractions(productService);
    }

    // ---------- helpers ----------

    private ProductRequest validRequest() {
        ProductRequest request = new ProductRequest();
        request.setCode("80216");
        request.setName("Terpel Celerity 4T Cuarto");
        request.setCategory(ProductCategory.LUBRICANTE);
        request.setPrice(new BigDecimal("28500"));
        request.setCost(new BigDecimal("21924"));
        request.setActive(true);
        return request;
    }

    private ResultActions postProduct(ProductRequest request) throws Exception {
        return mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions putProduct(Long id, ProductRequest request) throws Exception {
        return mockMvc.perform(put(BASE_URL + "/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }
}