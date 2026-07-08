package com.example.posoffline.data.seed

import com.example.posoffline.data.PosDatabase
import com.example.posoffline.data.repository.ProductRepository

/**
 * Seeds sample products on first run.
 * Idempotent: only writes if the store is empty.
 */
class SeedRunner(private val db: PosDatabase) {

    private val repo = ProductRepository(db.productDao())

    suspend fun ensureSeeded() {
        if (repo.count() > 0) return
        samples.forEach { s ->
            repo.create(
                sku = s.sku,
                name = s.name,
                price = s.price,
                stock = s.stock,
                category = s.category
            )
        }
    }

    private data class Sample(
        val sku: String,
        val name: String,
        val price: Long,
        val stock: Int,
        val category: String
    )

    private val samples = listOf(
        Sample("KOP-001", "Kopi Susu 250ml", 18000, 40, "Minuman"),
        Sample("TEH-001", "Teh Manis Panas", 8000, 60, "Minuman"),
        Sample("ROT-001", "Roti Coklat", 12000, 25, "Snack"),
        Sample("AIR-001", "Air Mineral 600ml", 5000, 100, "Minuman"),
        Sample("NGS-001", "Nasi Goreng Spesial", 25000, 20, "Makanan"),
        Sample("BUR-001", "Burger Beef", 32000, 15, "Makanan"),
        Sample("FRT-001", "Pisang Goreng", 10000, 30, "Snack"),
        Sample("JUS-001", "Jus Jeruk", 15000, 18, "Minuman")
    )
}
