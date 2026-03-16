package com.chrionline.server.service;

import com.chrionline.server.core.ServerLogger;
import com.chrionline.server.dao.ProductDAO;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;

import java.util.List;
import java.util.Map;

public class ProductService {

    private final ProductDAO productDAO = new ProductDAO();

    // GET_PRODUCTS — returns all available products
    public Response getProducts(Request request) {
        try {
            List<Map<String, Object>> products = productDAO.getAllProducts();
            ServerLogger.info("GET_PRODUCTS count=" + products.size());
            return Response.ok(products);  // data is the list directly
        } catch (Exception e) {
            ServerLogger.error("getProducts: " + e.getMessage());
            return Response.error("Failed to get products: " + e.getMessage());
        }
    }

    // GET_PRODUCT_DETAIL — returns one product by ID
    public Response getProductDetail(Request request) {
        try {
            int productId = request.getInt("productId");
            Map<String, Object> product = productDAO.getProductById(productId);
            if (product == null)
                return Response.error("Product not found");
            ServerLogger.info("GET_PRODUCT_DETAIL id=" + productId);
            return Response.ok(product);
        } catch (Exception e) {
            ServerLogger.error("getProductDetail: " + e.getMessage());
            return Response.error("Failed: " + e.getMessage());
        }
    }

    // SEARCH_PRODUCTS — finds products matching a keyword
    public Response searchProducts(Request request) {
        try {
            String keyword = request.getString("keyword");
            if (keyword == null || keyword.isEmpty())
                return Response.error("Keyword required");
            List<Map<String, Object>> results = productDAO.searchProducts(keyword);
            ServerLogger.info("SEARCH_PRODUCTS keyword='" + keyword +
                    "' results=" + results.size());
            return Response.ok(results);
        } catch (Exception e) {
            ServerLogger.error("searchProducts: " + e.getMessage());
            return Response.error("Failed: " + e.getMessage());
        }
    }
}
