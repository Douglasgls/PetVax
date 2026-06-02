package com.caogenda

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.caogenda.databinding.ItemVacinaBinding
import java.text.SimpleDateFormat
import java.util.Locale

class VacinaAdapter(private var vacinas: List<Vacina>) : RecyclerView.Adapter<VacinaAdapter.VacinaViewHolder>() {

    class VacinaViewHolder(val binding: ItemVacinaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VacinaViewHolder {
        val binding = ItemVacinaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VacinaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VacinaViewHolder, position: Int) {
        val vacina = vacinas[position]
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        
        with(holder.binding) {
            tvVacinaNome.text = vacina.nome
            tvVeterinario.text = vacina.veterinario
            tvDataAplicacao.text = if (vacina.dataAplicacao != 0L) sdf.format(vacina.dataAplicacao) else "-"
            tvProximaDose.text = if (vacina.proximaDose != 0L) sdf.format(vacina.proximaDose) else "-"
            tvStatusTag.text = vacina.status
            
            // Estilizar tag de status
            if (vacina.status == "Pendente") {
                tvStatusTag.setTextColor(android.graphics.Color.parseColor("#EA580C"))
            } else {
                tvStatusTag.setTextColor(android.graphics.Color.parseColor("#22C55E"))
            }

            root.setOnClickListener {
                val intent = Intent(it.context, VacinaDetailsActivity::class.java).apply {
                    putExtra("PET_ID", vacina.petId) // Note: needs to be set in Activity
                    putExtra("VACINA_ID", vacina.id)
                    putExtra("PET_NAME", vacina.nomePet)
                }
                it.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount() = vacinas.size

    fun updateList(newVacinas: List<Vacina>) {
        vacinas = newVacinas
        notifyDataSetChanged()
    }
}