package com.caogenda

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.caogenda.databinding.ItemPetBinding
import java.io.File



import android.graphics.drawable.Drawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.load.DataSource
import android.util.Log


class PetAdapter(private var pets: List<Pet>) : RecyclerView.Adapter<PetAdapter.PetViewHolder>() {

    class PetViewHolder(val binding: ItemPetBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val binding = ItemPetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        val pet = pets[position]
        with(holder.binding) {
            tvPetName.text = pet.nome
            tvEspecieTag.text = pet.especie
            tvPetInfo.text = "${pet.raca} • ${pet.calcularIdade()} • ${pet.peso} kg"
            
            val context = holder.itemView.context
            val fileName = pet.fotoPath
            
            val isActivityValid = (context as? Activity)?.let { !it.isDestroyed && !it.isFinishing } ?: true
            
            if (!fileName.isNullOrEmpty() && isActivityValid) {
                // BUG FIX: Standardized dynamic path resolution
                val file = if (fileName.contains("/")) File(fileName) 
                           else File(File(context.filesDir, "pets"), fileName)

                android.util.Log.d("PET_IMAGE", "fotoPath=$fileName")
                android.util.Log.d("PET_IMAGE", "absolutePath=${file.absolutePath}")
                android.util.Log.d("PET_IMAGE", "exists=${file.exists()}")
                android.util.Log.d("PET_IMAGE", "length=${if(file.exists()) file.length() else 0}")
                

                    /* ivPetPhoto.clearColorFilter()
                    ivPetPhoto.setImageURI(
                        android.net.Uri.fromFile(file)
                    )
                    Log.d(
                        "PET_IMAGE",
                        "width=${ivPetPhoto.width} height=${ivPetPhoto.height}"
                    )

                     */

                ivPetPhoto.clearColorFilter()

                Glide.with(context)
                    .load(file)
                    .placeholder(R.drawable.ic_pet_placeholder)
                    .error(R.drawable.ic_pet_placeholder)
                    .centerCrop()
                    .into(ivPetPhoto)

                    /* Glide.with(context)
                        .load(file)
                        .placeholder(R.drawable.ic_pet_placeholder)
                        .error(R.drawable.ic_pet_placeholder)
                        .listener(object : RequestListener<Drawable> {

                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>,
                                isFirstResource: Boolean
                            ): Boolean {
                                Log.e("PET_IMAGE", "Glide falhou", e)
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable,
                                model: Any,
                                target: Target<Drawable>?,
                                dataSource: DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                Log.d("PET_IMAGE", "Imagem carregada")
                                return false
                            }
                        })
                        .centerCrop()
                        .into(ivPetPhoto)

                     */

            } else {
                ivPetPhoto.setImageResource(R.drawable.ic_pet_placeholder)
                ivPetPhoto.setColorFilter(context.getColor(R.color.gray_light))
            }

            root.setOnClickListener {
                val intent = Intent(it.context, PetDetailsActivity::class.java).apply {
                    putExtra("PET_ID", pet.id)
                }
                it.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount() = pets.size

    fun updateList(newPets: List<Pet>) {
        pets = newPets
        notifyDataSetChanged()
    }
}