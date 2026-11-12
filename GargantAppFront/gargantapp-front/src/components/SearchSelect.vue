<script setup>
import { computed, nextTick, ref, useId } from 'vue';
import { filterOptions } from '../utils/search';

// Desplegable con búsqueda: se escribe para filtrar la lista. options = [{ value, label }].
// emptyLabel añade una primera opción con valor '' (p. ej. "Sin cliente" o "Todos").
const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  options: { type: Array, required: true },
  id: { type: String, default: undefined },
  placeholder: { type: String, default: 'Escribe para buscar...' },
  emptyLabel: { type: String, default: '' },
  required: { type: Boolean, default: false },
});
const emit = defineEmits(['update:modelValue']);

const listId = useId();
const open = ref(false);
const query = ref('');
const active = ref(0);

const selected = computed(() => props.options.find((option) => String(option.value) === String(props.modelValue)));
const selectedLabel = computed(() => selected.value?.label ?? (props.emptyLabel && props.modelValue === '' ? props.emptyLabel : ''));
const filtered = computed(() => [
  ...(props.emptyLabel && !query.value ? [{ value: '', label: props.emptyLabel }] : []),
  ...filterOptions(query.value, props.options),
]);
const optionId = (index) => `${listId}-${index}`;

const show = () => {
  open.value = true;
  query.value = '';
  active.value = Math.max(0, filtered.value.findIndex((option) => String(option.value) === String(props.modelValue)));
};

const close = () => {
  open.value = false;
  query.value = '';
};

const choose = (option) => {
  emit('update:modelValue', option.value);
  close();
};

const onInput = (event) => {
  query.value = event.target.value;
  open.value = true;
  active.value = 0;
};

const move = async (step) => {
  if (!open.value) return show();
  const last = filtered.value.length - 1;
  active.value = Math.min(last, Math.max(0, active.value + step));
  await nextTick();
  document.getElementById(optionId(active.value))?.scrollIntoView({ block: 'nearest' });
};

const onKeydown = (event) => {
  if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
    event.preventDefault();
    move(event.key === 'ArrowDown' ? 1 : -1);
  } else if (event.key === 'Enter' && open.value) {
    // Con la lista abierta, Enter elige la opción en lugar de enviar el formulario
    event.preventDefault();
    if (filtered.value[active.value]) choose(filtered.value[active.value]);
  } else if (event.key === 'Escape' && open.value) {
    event.preventDefault();
    close();
  }
};
</script>

<template>
  <div class="search-select">
    <input
      :id="id"
      type="text"
      role="combobox"
      autocomplete="off"
      aria-autocomplete="list"
      :aria-expanded="open"
      :aria-controls="listId"
      :aria-activedescendant="open && filtered[active] ? optionId(active) : undefined"
      :value="open ? query : selectedLabel"
      :placeholder="open && selectedLabel ? selectedLabel : placeholder"
      :required="required"
      @focus="show"
      @click="open || show()"
      @input="onInput"
      @keydown="onKeydown"
      @blur="close"
    />
    <ul v-if="open" :id="listId" role="listbox" class="search-select-list">
      <li
        v-for="(option, index) in filtered"
        :id="optionId(index)"
        :key="option.value"
        role="option"
        :aria-selected="String(option.value) === String(modelValue)"
        :class="{ active: index === active }"
        @mousedown.prevent="choose(option)"
        @mousemove="active = index"
      >
        {{ option.label }}
      </li>
      <li v-if="filtered.length === 0" class="search-select-empty">Sin resultados</li>
    </ul>
  </div>
</template>

<style scoped>
.search-select {
  position: relative;
}

.search-select input {
  padding-right: 34px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='8' viewBox='0 0 12 8'%3E%3Cpath d='M1 1l5 5 5-5' fill='none' stroke='%236b5e50' stroke-width='1.6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
}

.search-select-list {
  position: absolute;
  z-index: 20;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  max-height: 260px;
  overflow-y: auto;
  margin: 0;
  padding: 4px 0;
  list-style: none;
  background: var(--gargantua-white);
  border: 1px solid #cdbfa9;
  border-radius: var(--radius-sm);
  box-shadow: 0 6px 18px rgba(51, 33, 15, 0.15);
}

.search-select-list li {
  padding: 10px 12px;
  cursor: pointer;
  color: var(--gargantua-text);
}

.search-select-list li.active {
  background: var(--gargantua-cream);
}

.search-select-list li[aria-selected='true'] {
  font-weight: 600;
}

.search-select-empty {
  color: var(--gargantua-gray) !important;
  cursor: default !important;
}
</style>
