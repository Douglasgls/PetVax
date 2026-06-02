package com.caogenda

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.caogenda.databinding.ItemVacinaHomeBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class NextVacinaAdapter(private var vacinas: List<Vacina>) : RecyclerView.Adapter<NextVacinaAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemVacinaHomeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVacinaHomeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val vacina = vacinas[position]
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        
        with(holder.binding) {
            tvVacinaNome.text = vacina.nome
            tvPetEData.text = "${vacina.nomePet} • ${sdf.format(vacina.proximaDose)}"
            tvDiasRestantes.text = "${vacina.calcularDiasRestantes()}d"

            val context = holder.itemView.context
            val fileName = vacina.fotoPetPath
            
            val isActivityValid = (context as? Activity)?.let { !it.isDestroyed && !it.isFinishing } ?: true
            
            if (!fileName.isNullOrEmpty() && isActivityValid) {
                val file = if (fileName.contains("/")) File(fileName) 
                           else File(File(context.filesDir, "pets"), fileName)

                    /*ivPet.clearColorFilter()
                    ivPet.setImageURI(
                        android.net.Uri.fromFile(file)
                    )
                    Log.d(
                        "PET_IMAGE",
                        "width=${ivPet.width} height=${ivPet.height}"
                    )

                     */

                ivPet.clearColorFilter()

                Glide.with(context)
                    .load(file)
                    .placeholder(R.drawable.ic_pet_placeholder)
                    .error(R.drawable.ic_pet_placeholder)
                    .centerCrop()
                    .into(ivPet)
            } else {
                ivPet.setImageResource(R.drawable.ic_pet_placeholder)
                ivPet.setColorFilter(context.getColor(R.color.gray_light))
            }

            root.setOnClickListener {
                val intent = Intent(it.context, VacinaDetailsActivity::class.java).apply {
                    putExtra("PET_ID", vacina.petId)
                    putExtra("VACINA_ID", vacina.id)
                    putExtra("PET_NAME", vacina.nomePet)
                }
                it.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount() = vacinas.size

    fun updateList(newList: List<Vacina>) {
        vacinas = newList
        notifyDataSetChanged()
    }
}