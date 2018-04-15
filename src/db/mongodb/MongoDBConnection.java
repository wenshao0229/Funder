package db.mongodb;

import static com.mongodb.client.model.Filters.eq;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;

import db.DBConnection;
import entity.Item;
import entity.Item.ItemBuilder;
import external.ExternalAPI;
import external.ExternalAPIFactory;

public class MongoDBConnection implements DBConnection {
	private MongoClient mongoClient;
	private MongoDatabase db;

	public MongoDBConnection() {
		mongoClient = new MongoClient();
		db = mongoClient.getDatabase(MongoDBUtil.DB_NAME);
	}

	@Override
	public void close() {
		if (mongoClient != null) {
			mongoClient.close();
		}
	}

	@Override
	public void setFavoriteItems(String userId, List<String> itemIds) {
		db.getCollection("users").updateOne(eq("user_id", userId), eq("$push", eq("favorite", itemIds)));
	}

	@Override
	public void unsetFavoriteItems(String userId, List<String> itemIds) {
		db.getCollection("users").updateOne(eq("user_id", userId), eq("$pull", eq("favorite", itemIds)));
	}

	@Override
	public Set<String> getFavoriteItemIds(String userId) {
		Set<String> favoriteItems = new HashSet<>();
		FindIterable<Document> iterable = db.getCollection("user").find(eq("user_id", userId));
		if (iterable.first().containsKey("favorite")) {
			@SuppressWarnings("unchecked")
			List<String> list = (List<String>)iterable.first().get("favorite");
			favoriteItems.addAll(list);
		}
		return favoriteItems;
	}

	@Override
	public Set<Item> getFavoriteItems(String userId) {
		Set<Item> favoriteItems = new HashSet<>();
		Set<String> favoriteItemId = getFavoriteItemIds(userId);
		for (String itemId : favoriteItemId) {
			FindIterable<Document> iterable = db.getCollection("items").find(eq("item_id", itemId));
			Document doc = iterable.first();
			ItemBuilder builder = new ItemBuilder();
			builder.setItemId(doc.getString("item_id"));
			builder.setName(doc.getString("name"));
			builder.setCity(doc.getString("city"));
			builder.setState(doc.getString("state"));
			builder.setCountry(doc.getString("country"));
			builder.setZipcode(doc.getString("zipcode"));
			builder.setRating(doc.getDouble("rating"));
			builder.setAddress(doc.getString("address"));
			builder.setLatitude(doc.getDouble("latitude"));
			builder.setLongitude(doc.getDouble("longitude"));
			builder.setDescription(doc.getString("description"));
			builder.setSnippet(doc.getString("snippet"));
			builder.setSnippetUrl(doc.getString("snippet_url"));
			builder.setImageUrl(doc.getString("image_url"));
			builder.setUrl(doc.getString("url"));
			builder.setCategories(getCategories(itemId));
			favoriteItems.add(builder.build());
		}
		return favoriteItems;
	}

	@Override
	public Set<String> getCategories(String itemId) {
		Set<String> categories = new HashSet<>();
		FindIterable<Document> iterable = db.getCollection("items").find(eq("item_id", itemId));
		if (iterable.first().containsKey("categories") ) {
			@SuppressWarnings("unchecked")
			List<String> list = (List<String>)iterable.first().get("categories");
			categories.addAll(list);
		}
		return categories;
	}

	@Override
	public List<Item> searchItems(String userId, double lat, double lon, String term) {
		ExternalAPI api = ExternalAPIFactory.getExternalAPI();
		List<Item> items = api.search(lat, lon, term);
		for (Item item : items) {
			saveItem(item);
		}
		return items;
	}

	@Override
	public void saveItem(Item item) {
		FindIterable<Document> iterable = db.getCollection("items").find(eq("item_id", item.getItemId()));
		if (iterable.first() == null) {
			db.getCollection("items")
			.insertOne(new Document()
							.append("item_id", item.getItemId())
							.append("name", item.getName())
							.append("city", item.getCity())
							.append("state", item.getState())
							.append("country", item.getCountry())
							.append("zip_code", item.getZipcode())
							.append("rating", item.getRating())
							.append("address", item.getAddress())
							.append("latitude", item.getLatitude())
							.append("longitude", item.getLongitude())
							.append("description", item.getDescription())
							.append("snippet", item.getSnippet())
							.append("snippet_url", item.getSnippetUrl())
							.append("image_url", item.getImageUrl())
							.append("url", item.getUrl())
							.append("categories", item.getCategories()));
		}
	}

	@Override
	public String getFullname(String userId) {
		FindIterable<Document> iterable = db.getCollection("users").find(eq("user_id", userId));
		if (iterable.first() != null) {
			return iterable.first().get("first_name") + " " + iterable.first().get("last_name");
		}
		return null;
	}

	@Override
	public boolean verifyLogin(String userId, String password) {
		FindIterable<Document> iterable = db.getCollection("users").find(eq("user_id", userId));
		if (iterable.first() != null) {
			return iterable.first().get("password").equals(password);
		}
		return false;
	}
}
