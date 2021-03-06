package de.abas.training.advanced.transaction;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import de.abas.erp.db.DbContext;
import de.abas.erp.db.schema.part.Product;
import de.abas.erp.db.schema.part.ProductEditor;
import de.abas.erp.db.selection.Conditions;
import de.abas.erp.db.selection.SelectionBuilder;
import de.abas.training.advanced.common.ConnectionProvider;

public class CreateNewProductsFromXMLTransactionTest {

	@Rule
	public TestName testName = new TestName();
	private CreateNewProductsFromXMLTransaction instance;
	private DbContext ctx;

	/**
	 * Deletes all products that might be left from previous tests.
	 */
	@After
	public void cleanup() {
		deleteProducts("TESTHEAD");
		deleteProducts("TESTMYPC");
		deleteProducts("TESTROW");
	}

	@Test
	public void importOneProduct() {
		String input = "test/de/abas/training/advanced/transaction/example1.xml";
		instance.run(new String[] { "", input, getLogFileName() });
		SelectionBuilder<Product> selectionBuilder =
				SelectionBuilder.create(Product.class);
		selectionBuilder.add(Conditions.eq(Product.META.swd, "TESTHEAD"));
		List<Product> products = ctx.createQuery(selectionBuilder.build()).execute();
		assertEquals("count of products with swd TESTHEAD", 1, products.size());
	}

	@Test
	public void importProductWithRows() {
		String input = "test/de/abas/training/advanced/transaction/example2.xml";
		ProductEditor rowProduct = ctx.newObject(ProductEditor.class);
		rowProduct.setSwd("TESTROW");
		rowProduct.commit();
		instance.run(new String[] { "", input, getLogFileName() });

		SelectionBuilder<Product> selectionBuilder =
				SelectionBuilder.create(Product.class);
		selectionBuilder.add(Conditions.eq(Product.META.swd, "TESTHEAD"));
		List<Product> products = ctx.createQuery(selectionBuilder.build()).execute();
		assertEquals("count of products with swd TESTHEAD", 1, products.size());
		Product product = products.get(0);
		assertEquals("product has 1 row", 1, product.table().getRowCount());
		assertEquals("product row contains product TESTROW", "TESTROW", product
				.table().getRow(1).getProductListElem().getSwd());
	}

	@Before
	public void setup() {
		instance = new CreateNewProductsFromXMLTransaction();
		createClientContext();
		cleanup();
	}

	@Test
	public void shouldNotReimportExistingProduct() {
		String input = "test/de/abas/training/advanced/transaction/example1.xml";
		ProductEditor productEditor = ctx.newObject(ProductEditor.class);
		productEditor.setSwd("TESTHEAD");
		productEditor.setDescrOperLang("Test product for doublet check");
		productEditor.commit();
		instance.run(new String[] { "", input, getLogFileName() });
		SelectionBuilder<Product> selectionBuilder =
				SelectionBuilder.create(Product.class);
		selectionBuilder.add(Conditions.eq(Product.META.swd, "TESTHEAD"));
		List<Product> products = ctx.createQuery(selectionBuilder.build()).execute();
		assertEquals("count of products with swd TESTHEAD", 1, products.size());
		assertEquals("the product should have its original value in descrOperLang",
				"Test product for doublet check", products.get(0).getDescrOperLang());
	}

	/**
	 * Creates a client context with the standard port and predefined application
	 * name using the ajo-access.properties file.
	 */
	private void createClientContext() {
		ConnectionProvider connectionProvider = new ConnectionProvider();
		ctx = connectionProvider.createDbContext("test");
	}

	/**
	 * Deletes all products starting with the given search word.
	 *
	 * @param swd The search word prefix.
	 */
	private void deleteProducts(String swd) {
		SelectionBuilder<Product> selectionBuilder =
				SelectionBuilder.create(Product.class);
		selectionBuilder.add(Conditions.starts(Product.META.swd, swd));
		List<Product> products = ctx.createQuery(selectionBuilder.build()).execute();
		for (Product product : products) {
			product.delete();
		}
	}

	/**
	 * Defines path to and name of log file.
	 *
	 * @return The log file as instance of File.
	 */
	private String getLogFileName() {
		return new File("test/de/abas/training/advanced/transaction/"
				+ testName.getMethodName() + ".log").getAbsolutePath();
	}

}
