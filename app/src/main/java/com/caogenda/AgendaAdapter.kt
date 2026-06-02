package com.caogenda

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.caogenda.databinding.ItemHistoryYearBinding
import com.caogenda.databinding.ItemVacinaBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

sealed class AgendaItem {
    data class Header(val title: String) : AgendaItem()
    data class VacinaEntry(val vacina: Vacina) : AgendaItem()
}

class AgendaAdapter(
    private var items: List<AgendaItem>,
    private val onVacinaClick: (Vacina) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_VACINA = 1
    }

    override fun getItemViewType(position: Int) = when (items[position]) {
        is AgendaItem.Header -> TYPE_HEADER
        is AgendaItem.VacinaEntry -> TYPE_VACINA
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            val binding = ItemHistoryYearBinding.inflate(inflater, parent, false)
            HeaderViewHolder(binding)
        } else {
            val binding = ItemVacinaBinding.inflate(inflater, parent, false)
            VacinaViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (holder is HeaderViewHolder && item is AgendaItem.Header) {
            holder.binding.tvYear.text = item.title
        } else if (holder is VacinaViewHolder && item is AgendaItem.VacinaEntry) {
            val v = item.vacina
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            with(holder.binding) {
                tvVacinaNome.text = v.nome
                tvVeterinario.text = v.nomePet
                tvDataAplicacao.text = if (v.dataAplicacao != 0L) sdf.format(v.dataAplicacao) else "-"
                tvProximaDose.text = if (v.proximaDose != 0L) sdf.format(v.proximaDose) else "-"
                tvStatusTag.text = "Vence em ${v.calcularDiasRestantes()} dias"
                tvStatusTag.setTextColor(android.graphics.Color.parseColor("#EA580C"))
                
                val context = holder.itemView.context
                val fileName = v.fotoPetPath
                
                val isActivityValid = (context as? Activity)?.let { !it.isDestroyed && !it.isFinishing } ?: true

                if (!fileName.isNullOrEmpty() && isActivityValid) {
                    val file = if (fileName.contains("/")) File(fileName)
                               else File(File(context.filesDir, "pets"), fileName)

                    if (file.exists()) {
                        ivPetIcon.colorFilter = null
                        Glide.with(context)
                            .load(file)
                            .placeholder(R.drawable.ic_pet_placeholder)
                            .centerCrop()
                            .into(ivPetIcon)
                    } else {
                        ivPetIcon.setImageResource(R.drawable.ic_pet_placeholder)
                        ivPetIcon.setColorFilter(context.getColor(R.color.gray_light))
                    }
                } else {
                    ivPetIcon.setImageResource(R.drawable.ic_pet_placeholder)
                    ivPetIcon.setColorFilter(context.getColor(R.color.gray_light))
                }

                root.setOnClickListener { onVacinaClick(v) }
            }
        }
    }

    override fun getItemCount() = items.size

    fun updateData(vacinas: List<Vacina>) {
        val groupedItems = mutableListOf<AgendaItem>()
        val now = Calendar.getInstance()
        val today = now.clone() as Calendar
        val thisWeek = now.clone() as Calendar
        thisWeek.add(Calendar.DAY_OF_YEAR, 7)
        
        val todayList = mutableListOf<Vacina>()
        val weekList = mutableListOf<Vacina>()
        val futureList = mutableListOf<Vacina>()

        val itemCal = Calendar.getInstance()
        vacinas.forEach { v ->
            itemCal.timeInMillis = v.proximaDose
            
            if (isSameDay(itemCal, today)) {
                todayList.add(v)
            } else if (itemCal.before(thisWeek)) {
                weekList.add(v)
            } else {
                futureList.add(v)
            }
        }

        if (todayList.isNotEmpty()) {
            groupedItems.add(AgendaItem.Header("Hoje"))
            todayList.forEach { groupedItems.add(AgendaItem.VacinaEntry(it)) }
        }
        if (weekList.isNotEmpty()) {
            groupedItems.add(AgendaItem.Header("Esta Semana"))
            weekList.forEach { groupedItems.add(AgendaItem.VacinaEntry(it)) }
        }
        if (futureList.isNotEmpty()) {
            groupedItems.add(AgendaItem.Header("Próximos"))
            futureList.forEach { groupedItems.add(AgendaItem.VacinaEntry(it)) }
        }

        items = groupedItems
        notifyDataSetChanged()
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    class HeaderViewHolder(val binding: ItemHistoryYearBinding) : RecyclerView.ViewHolder(binding.root)
    class VacinaViewHolder(val binding: ItemVacinaBinding) : RecyclerView.ViewHolder(binding.root)
}