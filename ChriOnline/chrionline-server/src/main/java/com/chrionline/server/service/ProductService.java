package com.chrionline.server.service;

import com.chrionline.server.core.ServerLogger;
import com.chrionline.server.dao.CategoryDAO;
import com.chrionline.server.dao.ProductDAO;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ProductService {

    private final ProductDAO  productDAO  = new ProductDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

    public Response getProducts(Request request) {
        try {
            String token = request.getToken();
            String role = (token != null) ? SessionManager.getRole(token) : null;
            List<Map<String, Object>> products;
            if ("ADMIN".equals(role)) {
                products = productDAO.getAllProductsAdmin();
            } else {
                products = productDAO.getAllProducts();
            }
            ServerLogger.info("GET_PRODUCTS : " + products.size() + " produit(s)");
            return Response.ok("Produits r\u00e9cup\u00e9r\u00e9s.", products);
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
            return Response.ok("D\u00e9tail du produit.", product);
        } catch (SQLException e) {
            ServerLogger.error("GET_PRODUCT_DETAIL error : " + e.getMessage());
            return Response.error("Erreur serveur : " + e.getMessage());
        }
    }

    public Response searchProducts(Request request) {
        String keyword = request.getString("keyword");
        if (keyword == null || keyword.isEmpty())
            return Response.error("Mot-cl\u00e9 de recherche requis.");
        try {
            String token = request.getToken();
            String role = (token != null) ? SessionManager.getRole(token) : null;
            List<Map<String, Object>> results;
            if ("ADMIN".equals(role)) {
                results = productDAO.searchProductsAdmin(keyword);
            } else {
                results = productDAO.searchProducts(keyword);
            }
            ServerLogger.info("SEARCH_PRODUCTS '" + keyword + "' : "
                    + results.size() + " r\u00e9sultat(s)");
            return Response.ok("R\u00e9sultats de recherche.", results);
        } catch (SQLException e) {
            ServerLogger.error("SEARCH_PRODUCTS error : " + e.getMessage());
            return Response.error("Erreur serveur : " + e.getMessage());
        }
    }

    public Response getCategories(Request request) {
        try {
            List<Map<String, Object>> cats = categoryDAO.getAllCategories();
            ServerLogger.info("GET_CATEGORIES : " + cats.size() + " categorie(s)");
            return Response.ok("Categories r\u00e9cup\u00e9r\u00e9es.", cats);
        } catch (SQLException e) {
            ServerLogger.error("GET_CATEGORIES error : " + e.getMessage());
            return Response.error("Erreur serveur : " + e.getMessage());
        }
    }

    public Response addCategory(Request request) {
        String name = request.getString("name");
        String description = request.getString("description");
        if (name == null || name.trim().isEmpty())
            return Response.error("Le nom de la cat\u00e9gorie est requis.");
        try {
            Map<String, Object> created = categoryDAO.addCategory(
                    name.trim(), description != null ? description.trim() : "");
            ServerLogger.info("ADMIN_ADD_CATEGORY : " + name);
            return Response.ok("Cat\u00e9gorie ajout\u00e9e.", created);
        } catch (SQLException e) {
            ServerLogger.error("ADMIN_ADD_CATEGORY error : " + e.getMessage());
            return Response.error("Erreur serveur : " + e.getMessage());
        }
    }

    public Response editCategory(Request request) {
        int id = request.getInt("categoryId");
        String name = request.getString("name");
        String description = request.getString("description");
        if (id <= 0) return Response.error("ID cat\u00e9gorie invalide.");
        if (name == null || name.trim().isEmpty())
            return Response.error("Le nom est requis.");
        try {
            boolean ok = categoryDAO.editCategory(
                    id, name.trim(), description != null ? description.trim() : "");
            ServerLogger.info("ADMIN_EDIT_CATEGORY : ID=" + id);
            return ok ? Response.ok("Cat\u00e9gorie modifi\u00e9e.", null)
                      : Response.error("Cat\u00e9gorie introuvable.");
        } catch (SQLException e) {
            ServerLogger.error("ADMIN_EDIT_CATEGORY error : " + e.getMessage());
            return Response.error("Erreur serveur : " + e.getMessage());
        }
    }

    public Response deleteCategory(Request request) {
        int id = request.getInt("categoryId");
        if (id <= 0) return Response.error("ID cat\u00e9gorie invalide.");
        try {
            if (categoryDAO.isCategoryInUse(id))
                return Response.error(
                        "Impossible de supprimer : des produits utilisent cette cat\u00e9gorie.");
            boolean ok = categoryDAO.deleteCategory(id);
            ServerLogger.info("ADMIN_DELETE_CATEGORY : ID=" + id);
            return ok ? Response.ok("Cat\u00e9gorie supprim\u00e9e.", null)
                      : Response.error("Cat\u00e9gorie introuvable.");
        } catch (SQLException e) {
            ServerLogger.error("ADMIN_DELETE_CATEGORY error : " + e.getMessage());
            return Response.error("Erreur serveur : " + e.getMessage());
        }
    }
}