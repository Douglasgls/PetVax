package com.caogenda

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.caogenda.databinding.ItemPetSelectorBinding
import java.io.File

class PetSelectorAdapter(
    private var pets: List<Pet>,
    private val onPetSelected: (Pet) -> Unit
) : RecyclerView.Adapter<PetSelectorAdapter.ViewHolder>() {

    private var selectedPosition = 0

    class ViewHolder(val binding: ItemPetSelectorBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPetSelectorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pet = pets[position]
        val isSelected = position == selectedPosition
        
        with(holder.binding) {
            tvName.text = pet.nome
            tvEspecie.text = pet.especie
            tvIdade.text = pet.calcularIdade()
            
            val context = holder.itemView.context
            val fileName = pet.fotoPath
            
            val isActivityValid = (context as? Activity)?.let { !it.isDestroyed && !it.isFinishing } ?: true

            if (!fileName.isNullOrEmpty() && isActivityValid) {
                val file = if (fileName.contains("/")) File(fileName)
                           else File(File(context.filesDir, "pets"), fileName)

                if (file.exists()) {
                    ivPetPhoto.clearColorFilter()
                   // ivPetPhoto.setImageURI(
                    //    android.net.Uri.fromFile(file)
                    //)
                    //Log.d(
                    //  "PET_IMAGE",
                    //  "width=${ivPetPhoto.width} height=${ivPetPhoto.height}"
                    //)
                    // ivPetPhoto.colorFilter = null
                     Glide.with(context)
                        .load(file)
                        .placeholder(R.drawable.ic_pet_placeholder)
                        .centerCrop()
                        .into(ivPetPhoto)
                } else {
                    ivPetPhoto.setImageResource(R.drawable.ic_pet_placeholder)
                    ivPetPhoto.setColorFilter(context.getColor(R.color.gray_light))
                }
            } else {
                ivPetPhoto.setImageResource(R.drawable.ic_pet_placeholder)
                ivPetPhoto.setColorFilter(context.getColor(R.color.gray_light))
            }

            cardPet.strokeWidth = if (isSelected) 4 else 0
            cardPet.strokeColor = holder.itemView.context.getColor(R.color.primary_teal)
            
            root.setOnClickListener {
                val previous = selectedPosition
                selectedPosition = holder.adapterPosition
                notifyItemChanged(previous)
                notifyItemChanged(selectedPosition)
                onPetSelected(pet)
            }
        }
    }

    override fun getItemCount() = pets.size

    fun updateList(newPets: List<Pet>) {
        pets = newPets
        notifyDataSetChanged()
    }
}