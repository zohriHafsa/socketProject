package com.chrionline.server.service;

import com.chrionline.server.core.ServerLogger;
import com.chrionline.server.dao.ProductDAO;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ProductService {

    private final ProductDAO productDAO = new ProductDAO();

    public Response getProducts(Request request) {
        try {
            List<Map<String, Object>> products = productDAO.getAllProducts();
            ServerLogger.info("GET_PRODUCTS : " + products.size() + " produit(s)");
            return Response.ok("Produits récupérés.", products);
        } catch (SQLException e) {
            ServerLogger.error("GET_PRODUCTS error : " + e.getMessage());
            return Response.error("Erreur serveur : " + e.getMessage());
        }
    }

    public Response getProductDetail(Request request) {
        int productId = request.getInt("productId");
        if (productId <= 0)
            return Response.error("ID produit invalide.");
        try {
            Map<String, Object> product = productDAO.getProductById(productId);
            if (product == null)
                return Response.error("Produit introuvable (ID=" + productId + ").");
            ServerLogger.info("GET_PRODUCT_DETAIL : ID=" + productId);
            return Response.ok("Détail du produit.", product);
        } catch (SQLException e) {
            ServerLogger.error("GET_PRODUCT_DETAIL error : " + e.getMessage());
            return Response.error("Erreur serveur : " + e.getMessage());
        }
    }

    public Response searchProducts(Request request) {
        String keyword = request.getString("keyword");
        if (keyword == null || keyword.isEmpty())
            return Response.error("Mot-clé de recherche requis.");
        try {
            List<Map<String, Object>> results = productDAO.searchProducts(keyword);
            ServerLogger.info("SEARCH_PRODUCTS '" + keyword + "' : "
                    + results.size() + " résultat(s)");
            return Response.ok("Résultats de recherche.", results);
        } catch (SQLException e) {
            ServerLogger.error("SEARCH_PRODUCTS error : " + e.getMessage());
            return Response.error("Erreur serveur : " + e.getMessage());
        }
    }
}