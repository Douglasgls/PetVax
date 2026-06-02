package com.caogenda

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.caogenda.databinding.ItemHistoryYearBinding
import com.caogenda.databinding.ItemVacinaBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

sealed class HistoryItem {
    data class YearHeader(val year: Int) : HistoryItem()
    data class VacinaItem(val vacina: Vacina) : HistoryItem()
}

class HistoricoAdapter(
    private var items: List<HistoryItem>,
    private val onVacinaClick: (Vacina) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_YEAR = 0
        private const val TYPE_VACINA = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is HistoryItem.YearHeader -> TYPE_YEAR
            is HistoryItem.VacinaItem -> TYPE_VACINA
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_YEAR) {
            val binding = ItemHistoryYearBinding.inflate(inflater, parent, false)
            YearViewHolder(binding)
        } else {
            val binding = ItemVacinaBinding.inflate(inflater, parent, false)
            VacinaViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (holder is YearViewHolder && item is HistoryItem.YearHeader) {
            holder.binding.tvYear.text = item.year.toString()
        } else if (holder is VacinaViewHolder && item is HistoryItem.VacinaItem) {
            val v = item.vacina
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            with(holder.binding) {
                tvVacinaNome.text = v.nome
                tvVeterinario.text = v.veterinario
                tvDataAplicacao.text = if (v.dataAplicacao != 0L) sdf.format(v.dataAplicacao) else "-"
                tvProximaDose.text = if (v.proximaDose != 0L) sdf.format(v.proximaDose) else "-"
                tvStatusTag.text = v.status
                
                if (v.status == "Pendente") {
                    tvStatusTag.setTextColor(android.graphics.Color.parseColor("#EA580C"))
                } else {
                    tvStatusTag.setTextColor(android.graphics.Color.parseColor("#22C55E"))
                }

                root.setOnClickListener { onVacinaClick(v) }
            }
        }
    }

    override fun getItemCount() = items.size

    fun updateData(vacinas: List<Vacina>) {
        val groupedItems = mutableListOf<HistoryItem>()
        val calendar = Calendar.getInstance()
        
        // Agrupar por ano
        val groups = vacinas.groupBy {
            calendar.timeInMillis = it.dataAplicacao
            calendar.get(Calendar.YEAR)
        }.toSortedMap(compareByDescending { it })

        groups.forEach { (year, list) ->
            groupedItems.add(HistoryItem.YearHeader(year))
            list.sortedByDescending { it.dataAplicacao }.forEach {
                groupedItems.add(HistoryItem.VacinaItem(it))
            }
        }

        items = groupedItems
        notifyDataSetChanged()
    }

    class YearViewHolder(val binding: ItemHistoryYearBinding) : RecyclerView.ViewHolder(binding.root)
    class VacinaViewHolder(val binding: ItemVacinaBinding) : RecyclerView.ViewHolder(binding.root)
}