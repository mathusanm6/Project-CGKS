import React, { useState, useRef } from 'react';
import { XIcon } from '../icons/Icons';

const TagInput = ({ value, onChange, placeholder, name, id, isInvalid }) => {
  const [inputValue, setInputValue] = useState('');
  const inputRef = useRef(null);
  const containerRef = useRef(null);
  
  // Parse comma-separated values into array of tags
  const tags = value ? value.split(',').filter(tag => tag.trim() !== '') : [];
  
  const handleInputChange = (e) => {
    setInputValue(e.target.value);
  };
  
  const handleKeyDown = (e) => {
    // If user presses Enter or comma, add a new tag
    if ((e.key === 'Enter' || e.key === ',') && inputValue.trim()) {
      e.preventDefault();
      addTag();
    } 
    // If Backspace is pressed and input is empty, remove the last tag
    else if (e.key === 'Backspace' && inputValue === '' && tags.length > 0) {
      removeTag(tags.length - 1);
    }
  };
  
  const addTag = () => {
    // Clean the input (remove any commas and trim whitespace)
    const newTag = inputValue.replace(/,/g, '').trim();
    
    // Only add the tag if it doesn't already exist and is a valid number
    if (newTag && !tags.includes(newTag) && /^\d+$/.test(newTag)) {
      const newTags = [...tags, newTag];
      onChange({ 
        target: { 
          name, 
          value: newTags.join(',') 
        } 
      });
      setInputValue('');
    } else {
      setInputValue('');
    }
  };
  
  const removeTag = (index) => {
    const newTags = [...tags];
    newTags.splice(index, 1);
    onChange({ 
      target: { 
        name, 
        value: newTags.join(',') 
      } 
    });
  };
  
  const handleContainerClick = () => {
    inputRef.current.focus();
  };
  
  const handleBlur = () => {
    // When focus leaves the input, add any remaining text as a tag
    if (inputValue.trim()) {
      addTag();
    }
  };
  
  return (
    <div 
      ref={containerRef}
      className={`tag-input-container ${isInvalid ? 'error' : ''}`}
      onClick={handleContainerClick}
      aria-labelledby={`${id}-label`}
    >
      {tags.map((tag, index) => (
        <div key={index} className="tag">
          {tag}
          <button 
            type="button"
            className="tag-delete"
            onClick={() => removeTag(index)}
            aria-label={`Supprimer l'item ${tag}`}
          >
            <XIcon />
          </button>
        </div>
      ))}
      <input
        ref={inputRef}
        id={id}
        type="text"
        className="tag-input"
        value={inputValue}
        onChange={handleInputChange}
        onKeyDown={handleKeyDown}
        onBlur={handleBlur}
        placeholder={tags.length === 0 ? placeholder : ""}
        aria-invalid={isInvalid ? "true" : "false"}
      />
    </div>
  );
};

export default TagInput;