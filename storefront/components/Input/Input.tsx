import { FC, FormEvent } from "react";

type Props = {
  type?: string;
  name: string;
  placeholder?: string;
  extraClass?: string;
  required?: boolean;
  border?: string;
  id?: string;
  label?: string;
  onChange?: (e: FormEvent<HTMLInputElement>) => void;
  value?: string;
  readOnly?: boolean;
  disabled?: boolean;
};

const Input: FC<Props> = ({
  type = "text",
  name,
  placeholder,
  extraClass,
  required = false,
  border = "",
  label = "",
  onChange,
  value,
  readOnly = false,
  disabled = false,
}) => (
  <input
    type={type}
    readOnly={readOnly}
    disabled={disabled}
    className={`${
      border !== "" ? border : "border-2 border-gray500"
    } py-2 px-4 outline-none ${extraClass}`}
    name={name}
    placeholder={placeholder}
    required={required}
    onChange={onChange}
    value={value}
    aria-label={label}
  />
);

export default Input;
